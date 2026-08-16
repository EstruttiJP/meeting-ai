package com.meetingai.backend.summary;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetingai.backend.meeting.Meeting;
import com.meetingai.backend.meeting.MeetingNotFoundException;
import com.meetingai.backend.meeting.MeetingRepository;
import com.meetingai.backend.user.User;

import tools.jackson.databind.ObjectMapper;

/**
 * Leitura e edição/aprovação do resumo — o PUT é a própria revisão humana
 * (edita e aprova numa tacada só, já que não existe endpoint de "aprovar"
 * separado no contrato de API). Nunca envia nada ao CRM por conta própria.
 */
@Service
public class SummaryService {

	private final SummaryRepository summaryRepository;
	private final MeetingRepository meetingRepository;
	private final ObjectMapper objectMapper;

	public SummaryService(SummaryRepository summaryRepository, MeetingRepository meetingRepository, ObjectMapper objectMapper) {
		this.summaryRepository = summaryRepository;
		this.meetingRepository = meetingRepository;
		this.objectMapper = objectMapper;
	}

	@Transactional(readOnly = true)
	public SummaryResponse getForMeeting(User user, UUID meetingId) {
		Meeting meeting = getOwnedMeeting(user, meetingId);
		return toResponse(getSummaryOrThrow(meeting.getId()));
	}

	@Transactional
	public SummaryResponse approve(User user, UUID meetingId, SummaryContent content) {
		Meeting meeting = getOwnedMeeting(user, meetingId);
		Summary summary = getSummaryOrThrow(meeting.getId());
		summary.updateContent(objectMapper.writeValueAsString(content));
		summary.approve();
		return toResponse(summaryRepository.save(summary));
	}

	@Transactional
	public SummaryResponse updateSummaryText(User user, UUID meetingId, String text) {
		return mutate(user, meetingId, content -> new SummaryContent(text, content.items()));
	}

	/**
	 * O id é gerado aqui, e não no cliente, para não colidir com os ids que o
	 * modelo já produziu — dois itens com o mesmo id fariam a edição granular
	 * atingir o item errado.
	 */
	@Transactional
	public SummaryResponse addItem(User user, UUID meetingId, SummaryItemCreateRequest request) {
		return mutate(user, meetingId, content -> {
			List<SummaryItem> items = new ArrayList<>(content.items());
			// Item anotado à mão entra como normal; quem quiser destacá-lo usa o
			// PATCH de prioridade, como faria com um item vindo do modelo.
			SummaryItem novo = new SummaryItem(nextItemId(content), request.type(), request.content(),
					request.timestampSeconds(), null);
			items.add(novo.canBeHighlighted()
					? new SummaryItem(novo.id(), novo.type(), novo.content(), novo.timestampSeconds(),
							SummaryItemPriority.NORMAL)
					: novo);
			return new SummaryContent(content.summary(), items);
		});
	}

	@Transactional
	public SummaryResponse updateItem(User user, UUID meetingId, String itemId, SummaryItemUpdateRequest request) {
		if (request.isEmpty()) {
			throw new SummaryItemUpdateException("Informe ao menos um campo para alterar no item.");
		}
		if (request.content() != null && request.content().isBlank()) {
			throw new SummaryItemUpdateException("O conteúdo do item não pode ficar vazio.");
		}
		return mutate(user, meetingId, content -> {
			SummaryItem target = content.items().stream()
					.filter(item -> item.id().equals(itemId))
					.findFirst()
					.orElseThrow(() -> new SummaryItemNotFoundException(itemId));
			if (request.priority() != null && !target.canBeHighlighted()) {
				throw new SummaryItemUpdateException(
						"Só decisão e próximo passo têm prioridade.");
			}
			List<SummaryItem> items = content.items().stream()
					.map(item -> item.id().equals(itemId) ? applyTo(item, request) : item)
					.toList();
			return new SummaryContent(content.summary(), items);
		});
	}

	/** Campo nulo no request significa "não mexe", não "apaga". */
	private static SummaryItem applyTo(SummaryItem item, SummaryItemUpdateRequest request) {
		return new SummaryItem(
				item.id(),
				item.type(),
				request.content() == null ? item.content() : request.content(),
				item.timestampSeconds(),
				request.priority() == null ? item.priority() : request.priority());
	}

	@Transactional
	public SummaryResponse removeItem(User user, UUID meetingId, String itemId) {
		return mutate(user, meetingId, content -> {
			List<SummaryItem> items = content.items().stream()
					.filter(item -> !item.id().equals(itemId))
					.toList();
			if (items.size() == content.items().size()) {
				throw new SummaryItemNotFoundException(itemId);
			}
			return new SummaryContent(content.summary(), items);
		});
	}

	/**
	 * Lê, transforma e regrava o JSON do resumo. A edição granular não reaprova
	 * nada: mexer num item depois de aprovado devolve o resumo para revisão, em
	 * vez de deixar passar como se já tivesse sido conferido.
	 */
	private SummaryResponse mutate(User user, UUID meetingId, UnaryOperator<SummaryContent> change) {
		Meeting meeting = getOwnedMeeting(user, meetingId);
		Summary summary = getSummaryOrThrow(meeting.getId());
		SummaryContent current = objectMapper.readValue(summary.getContent(), SummaryContent.class);
		summary.updateContent(objectMapper.writeValueAsString(change.apply(current)));
		return toResponse(summaryRepository.save(summary));
	}

	private String nextItemId(SummaryContent content) {
		Set<String> used = content.items().stream().map(SummaryItem::id).collect(Collectors.toSet());
		int candidate = content.items().size() + 1;
		while (!used.add("item-" + candidate)) {
			candidate++;
		}
		return "item-" + candidate;
	}

	private Summary getSummaryOrThrow(UUID meetingId) {
		return summaryRepository.findByMeetingId(meetingId)
				.orElseThrow(() -> new SummaryNotFoundException(meetingId));
	}

	private Meeting getOwnedMeeting(User user, UUID meetingId) {
		Meeting meeting = meetingRepository.findById(meetingId)
				.orElseThrow(() -> new MeetingNotFoundException(meetingId));
		if (!meeting.getUser().getId().equals(user.getId())) {
			// Não revela pra quem não é dono que o id existe.
			throw new MeetingNotFoundException(meetingId);
		}
		return meeting;
	}

	private SummaryResponse toResponse(Summary summary) {
		SummaryContent content = objectMapper.readValue(summary.getContent(), SummaryContent.class);
		return new SummaryResponse(
				summary.getId(),
				summary.getMeeting().getId(),
				content,
				summary.isApproved(),
				summary.getApprovedAt());
	}

}
