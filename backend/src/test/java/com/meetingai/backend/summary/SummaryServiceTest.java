package com.meetingai.backend.summary;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.meetingai.backend.meeting.Meeting;
import com.meetingai.backend.meeting.MeetingNotFoundException;
import com.meetingai.backend.meeting.MeetingRepository;
import com.meetingai.backend.user.User;

import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class SummaryServiceTest {

	@Mock
	private SummaryRepository summaryRepository;

	@Mock
	private MeetingRepository meetingRepository;

	private SummaryService summaryService;
	private User user;
	private Meeting meeting;
	private UUID meetingId;

	@BeforeEach
	void setUp() {
		summaryService = new SummaryService(summaryRepository, meetingRepository, JsonMapper.builder().build());
		user = new User("google-sub-1", "dev@meetingai.com", "Dev User", null);
		ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
		meeting = new Meeting(user, "Reunião de vendas", "reuniao.mp3", "user-1/key.mp3");
		meetingId = UUID.randomUUID();
		ReflectionTestUtils.setField(meeting, "id", meetingId);
	}

	@Test
	void getForMeetingReturnsDeserializedContentWhenOwned() {
		String json = "{\"summary\":\"resumo salvo\",\"items\":[]}";
		Summary summary = new Summary(meeting, json);
		given(meetingRepository.findById(meetingId)).willReturn(Optional.of(meeting));
		given(summaryRepository.findByMeetingId(meetingId)).willReturn(Optional.of(summary));

		SummaryResponse response = summaryService.getForMeeting(user, meetingId);

		assertThat(response.content().summary()).isEqualTo("resumo salvo");
		assertThat(response.meetingId()).isEqualTo(meetingId);
	}

	@Test
	void getForMeetingThrowsWhenMeetingDoesNotExist() {
		given(meetingRepository.findById(meetingId)).willReturn(Optional.empty());

		assertThatThrownBy(() -> summaryService.getForMeeting(user, meetingId))
				.isInstanceOf(MeetingNotFoundException.class);
	}

	@Test
	void getForMeetingThrowsWhenMeetingBelongsToAnotherUser() {
		User otherUser = new User("google-sub-2", "other@meetingai.com", "Other User", null);
		ReflectionTestUtils.setField(otherUser, "id", UUID.randomUUID());
		Meeting othersMeeting = new Meeting(otherUser, "Reunião de outro usuário", "r.mp3", "key.mp3");
		given(meetingRepository.findById(meetingId)).willReturn(Optional.of(othersMeeting));

		assertThatThrownBy(() -> summaryService.getForMeeting(user, meetingId))
				.isInstanceOf(MeetingNotFoundException.class);
	}

	@Test
	void getForMeetingThrowsWhenSummaryNotReadyYet() {
		given(meetingRepository.findById(meetingId)).willReturn(Optional.of(meeting));
		given(summaryRepository.findByMeetingId(meetingId)).willReturn(Optional.empty());

		assertThatThrownBy(() -> summaryService.getForMeeting(user, meetingId))
				.isInstanceOf(SummaryNotFoundException.class);
	}

	@Test
	void approveUpdatesContentAndMarksApproved() {
		String originalJson = "{\"summary\":\"original\",\"items\":[]}";
		Summary summary = new Summary(meeting, originalJson);
		given(meetingRepository.findById(meetingId)).willReturn(Optional.of(meeting));
		given(summaryRepository.findByMeetingId(meetingId)).willReturn(Optional.of(summary));
		given(summaryRepository.save(any(Summary.class))).willAnswer(invocation -> invocation.getArgument(0));
		SummaryContent editedContent = new SummaryContent("resumo editado", List.of());

		SummaryResponse response = summaryService.approve(user, meetingId, editedContent);

		assertThat(response.content().summary()).isEqualTo("resumo editado");
		assertThat(response.approved()).isTrue();
		assertThat(response.approvedAt()).isNotNull();
		assertThat(summary.isApproved()).isTrue();
	}

	@Test
	void updateItemChangesOnlyTheTargetItemAndKeepsItsTimestamp() {
		givenSummaryWithItems();
		givenSavePersists();

		SummaryResponse response = summaryService.updateItem(user, meetingId, "item-2", new SummaryItemUpdateRequest("Decisão corrigida", null));

		assertThat(response.content().items()).satisfiesExactly(
				first -> assertThat(first.content()).isEqualTo("Primeiro item"),
				second -> {
					assertThat(second.content()).isEqualTo("Decisão corrigida");
					// timestamp preservado: continua apontando pro mesmo ponto do áudio
					assertThat(second.timestampSeconds()).isEqualTo(30.0);
					assertThat(second.type()).isEqualTo(SummaryItemType.DECISAO);
				},
				third -> assertThat(third.content()).isEqualTo("R$ 500"));
	}

	@Test
	void removeItemDropsOnlyThatItem() {
		givenSummaryWithItems();
		givenSavePersists();

		SummaryResponse response = summaryService.removeItem(user, meetingId, "item-1");

		assertThat(response.content().items()).map(SummaryItem::id)
				.containsExactly("item-2", "item-3");
	}

	@Test
	void addItemGeneratesAnIdThatDoesNotCollideWithExistingOnes() {
		givenSummaryWithItems();
		givenSavePersists();

		SummaryResponse response = summaryService.addItem(user, meetingId,
				new SummaryItemCreateRequest(SummaryItemType.PONTO_ATENCAO, "Anotado à mão", 12.0));

		assertThat(response.content().items()).hasSize(4);
		assertThat(response.content().items()).map(SummaryItem::id).doesNotHaveDuplicates();
		assertThat(response.content().itemsOfType(SummaryItemType.PONTO_ATENCAO))
				.singleElement()
				.satisfies(item -> assertThat(item.content()).isEqualTo("Anotado à mão"));
	}

	@Test
	void editingItemsDoesNotTouchTheSummaryText() {
		givenSummaryWithItems();
		givenSavePersists();

		SummaryResponse response = summaryService.removeItem(user, meetingId, "item-1");

		assertThat(response.content().summary()).isEqualTo("resumo original");
	}

	@Test
	void promotesAnItemToHighPriorityWithoutTouchingItsText() {
		givenSummaryWithItems();
		givenSavePersists();

		SummaryResponse response = summaryService.updateItem(user, meetingId, "item-2",
				new SummaryItemUpdateRequest(null, SummaryItemPriority.ALTA));

		SummaryItem alterado = response.content().items().stream()
				.filter(i -> i.id().equals("item-2")).findFirst().orElseThrow();
		assertThat(alterado.priority()).isEqualTo(SummaryItemPriority.ALTA);
		assertThat(alterado.content()).isEqualTo("Segundo item");
	}

	@Test
	void demotesAnItemBackToNormal() {
		givenSummaryWithItems();
		givenSavePersists();

		summaryService.updateItem(user, meetingId, "item-2",
				new SummaryItemUpdateRequest(null, SummaryItemPriority.ALTA));
		SummaryResponse response = summaryService.updateItem(user, meetingId, "item-2",
				new SummaryItemUpdateRequest(null, SummaryItemPriority.NORMAL));

		assertThat(response.content().items().stream()
				.filter(i -> i.id().equals("item-2")).findFirst().orElseThrow().priority())
				.isEqualTo(SummaryItemPriority.NORMAL);
	}

	@Test
	void editingOnlyTheTextKeepsThePriorityAsItWas() {
		givenSummaryWithItems();
		givenSavePersists();

		summaryService.updateItem(user, meetingId, "item-2",
				new SummaryItemUpdateRequest(null, SummaryItemPriority.ALTA));
		SummaryResponse response = summaryService.updateItem(user, meetingId, "item-2",
				new SummaryItemUpdateRequest("Texto novo", null));

		SummaryItem alterado = response.content().items().stream()
				.filter(i -> i.id().equals("item-2")).findFirst().orElseThrow();
		assertThat(alterado.content()).isEqualTo("Texto novo");
		assertThat(alterado.priority()).isEqualTo(SummaryItemPriority.ALTA);
	}

	@Test
	void refusesPriorityOnATypeThatDoesNotHaveOne() {
		givenSummaryWithItems();

		// item-3 é um valor_mencionado: não disputa o card de destaque.
		assertThatThrownBy(() -> summaryService.updateItem(user, meetingId, "item-3",
				new SummaryItemUpdateRequest(null, SummaryItemPriority.ALTA)))
				.isInstanceOf(SummaryItemUpdateException.class);
	}

	@Test
	void refusesAnUpdateThatChangesNothing() {
		// Validado antes de tocar no banco, então nem precisa de resumo carregado.
		assertThatThrownBy(() -> summaryService.updateItem(user, meetingId, "item-2",
				new SummaryItemUpdateRequest(null, null)))
				.isInstanceOf(SummaryItemUpdateException.class);
	}

	@Test
	void refusesToBlankOutAnItem() {
		assertThatThrownBy(() -> summaryService.updateItem(user, meetingId, "item-2",
				new SummaryItemUpdateRequest("   ", null)))
				.isInstanceOf(SummaryItemUpdateException.class);
	}

	@Test
	void manuallyAddedDecisionStartsAsNormalPriority() {
		givenSummaryWithItems();
		givenSavePersists();

		SummaryResponse response = summaryService.addItem(user, meetingId,
				new SummaryItemCreateRequest(SummaryItemType.DECISAO, "Anotado à mão", 12.0));

		assertThat(response.content().itemsOfType(SummaryItemType.DECISAO))
				.filteredOn(i -> i.content().equals("Anotado à mão"))
				.singleElement()
				.satisfies(i -> assertThat(i.priority()).isEqualTo(SummaryItemPriority.NORMAL));
	}

	@Test
	void manuallyAddedValueHasNoPriorityAtAll() {
		givenSummaryWithItems();
		givenSavePersists();

		SummaryResponse response = summaryService.addItem(user, meetingId,
				new SummaryItemCreateRequest(SummaryItemType.VALOR_MENCIONADO, "R$ 10.000", 12.0));

		assertThat(response.content().itemsOfType(SummaryItemType.VALOR_MENCIONADO))
				.filteredOn(i -> i.content().equals("R$ 10.000"))
				.singleElement()
				.satisfies(i -> assertThat(i.priority()).isNull());
	}

	@Test
	void updateItemThrowsWhenItemDoesNotExist() {
		givenSummaryWithItems();

		assertThatThrownBy(() -> summaryService.updateItem(user, meetingId, "nao-existe", new SummaryItemUpdateRequest("x", null)))
				.isInstanceOf(SummaryItemNotFoundException.class);
	}

	@Test
	void removeItemThrowsWhenItemDoesNotExist() {
		givenSummaryWithItems();

		assertThatThrownBy(() -> summaryService.removeItem(user, meetingId, "nao-existe"))
				.isInstanceOf(SummaryItemNotFoundException.class);
	}

	@Test
	void updateSummaryTextKeepsTheItemsUntouched() {
		givenSummaryWithItems();
		givenSavePersists();

		SummaryResponse response = summaryService.updateSummaryText(user, meetingId, "resumo reescrito");

		assertThat(response.content().summary()).isEqualTo("resumo reescrito");
		assertThat(response.content().items()).hasSize(3);
	}

	private Summary givenSummaryWithItems() {
		String json = """
				{"summary":"resumo original","items":[
				  {"id":"item-1","type":"proximo_passo","content":"Primeiro item","timestampSeconds":10.0,"priority":"normal"},
				  {"id":"item-2","type":"decisao","content":"Segundo item","timestampSeconds":30.0,"priority":"normal"},
				  {"id":"item-3","type":"valor_mencionado","content":"R$ 500","timestampSeconds":50.0}
				]}
				""";
		Summary summary = new Summary(meeting, json);
		given(meetingRepository.findById(meetingId)).willReturn(Optional.of(meeting));
		given(summaryRepository.findByMeetingId(meetingId)).willReturn(Optional.of(summary));
		return summary;
	}

	/**
	 * Fica fora do helper acima de propósito: os testes de item inexistente não
	 * podem salvar nada, e o Mockito acusa a stub não usada se ela vier junto.
	 */
	private void givenSavePersists() {
		given(summaryRepository.save(any(Summary.class))).willAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	void approveThrowsWhenMeetingBelongsToAnotherUser() {
		User otherUser = new User("google-sub-2", "other@meetingai.com", "Other User", null);
		ReflectionTestUtils.setField(otherUser, "id", UUID.randomUUID());
		Meeting othersMeeting = new Meeting(otherUser, "Reunião de outro usuário", "r.mp3", "key.mp3");
		given(meetingRepository.findById(meetingId)).willReturn(Optional.of(othersMeeting));
		SummaryContent editedContent = new SummaryContent("resumo editado", List.of());

		assertThatThrownBy(() -> summaryService.approve(user, meetingId, editedContent))
				.isInstanceOf(MeetingNotFoundException.class);
	}

}
