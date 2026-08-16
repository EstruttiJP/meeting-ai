package com.meetingai.backend.crm;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetingai.backend.crypto.EncryptionService;
import com.meetingai.backend.meeting.Meeting;
import com.meetingai.backend.meeting.MeetingAccessService;
import com.meetingai.backend.meeting.MeetingRepository;
import com.meetingai.backend.summary.Summary;
import com.meetingai.backend.summary.SummaryContent;
import com.meetingai.backend.summary.SummaryItem;
import com.meetingai.backend.summary.SummaryItemType;
import com.meetingai.backend.summary.SummaryNotApprovedException;
import com.meetingai.backend.summary.SummaryNotFoundException;
import com.meetingai.backend.summary.SummaryRepository;
import com.meetingai.backend.user.User;

import tools.jackson.databind.ObjectMapper;

/**
 * Só dispara a partir de aprovação explícita do usuário (nunca automático) —
 * pega o resumo já aprovado e cria um negócio no Pipedrive com uma nota
 * contendo o conteúdo estruturado.
 */
@Service
public class CrmSyncService {

	private final MeetingAccessService meetingAccessService;
	private final MeetingRepository meetingRepository;
	private final SummaryRepository summaryRepository;
	private final CrmConnectionRepository crmConnectionRepository;
	private final EncryptionService encryptionService;
	private final PipedriveClient pipedriveClient;
	private final ObjectMapper objectMapper;

	public CrmSyncService(MeetingAccessService meetingAccessService,
			MeetingRepository meetingRepository,
			SummaryRepository summaryRepository,
			CrmConnectionRepository crmConnectionRepository,
			EncryptionService encryptionService,
			PipedriveClient pipedriveClient,
			ObjectMapper objectMapper) {
		this.meetingAccessService = meetingAccessService;
		this.meetingRepository = meetingRepository;
		this.summaryRepository = summaryRepository;
		this.crmConnectionRepository = crmConnectionRepository;
		this.encryptionService = encryptionService;
		this.pipedriveClient = pipedriveClient;
		this.objectMapper = objectMapper;
	}

	@Transactional
	public void sendApprovedSummaryToCrm(User user, UUID meetingId) {
		Meeting meeting = meetingAccessService.getOwnedMeeting(user, meetingId);
		Summary summary = summaryRepository.findByMeetingId(meeting.getId())
				.orElseThrow(() -> new SummaryNotFoundException(meetingId));
		if (!summary.isApproved()) {
			throw new SummaryNotApprovedException(meetingId);
		}
		CrmConnection connection = crmConnectionRepository.findByUserId(user.getId())
				.orElseThrow(() -> new CrmConnectionException(
						"Nenhuma conexão de CRM configurada. Conecte o Pipedrive em Configurações."));

		String accessToken = encryptionService.decrypt(connection.getAccessTokenEncrypted());
		SummaryContent content = objectMapper.readValue(summary.getContent(), SummaryContent.class);

		long dealId = pipedriveClient.createDeal(accessToken, meeting.getTitle());
		pipedriveClient.addNote(accessToken, dealId, formatNote(content));

		meeting.markSentToCrm();
		meetingRepository.save(meeting);
	}

	/**
	 * A nota do CRM continua agrupada por tipo, ao contrário da tela de revisão:
	 * quem lê no Pipedrive quer as decisões juntas, não a ordem cronológica da
	 * conversa. Os timestamps não vão na nota — só fazem sentido com o áudio ao
	 * lado, que o CRM não tem.
	 */
	private String formatNote(SummaryContent content) {
		StringBuilder note = new StringBuilder();
		note.append(content.summary()).append("\n\n");
		appendSection(note, "Decisões", content.itemsOfType(SummaryItemType.DECISAO));
		appendSection(note, "Próximos passos", content.itemsOfType(SummaryItemType.PROXIMO_PASSO));
		appendSection(note, "Valores mencionados", content.itemsOfType(SummaryItemType.VALOR_MENCIONADO));
		appendSection(note, "Pontos de atenção", content.itemsOfType(SummaryItemType.PONTO_ATENCAO));
		return note.toString();
	}

	private void appendSection(StringBuilder note, String title, List<SummaryItem> items) {
		if (items == null || items.isEmpty()) {
			return;
		}
		note.append(title).append(":\n");
		items.forEach(item -> note.append("- ").append(item.content()).append('\n'));
		note.append('\n');
	}

}
