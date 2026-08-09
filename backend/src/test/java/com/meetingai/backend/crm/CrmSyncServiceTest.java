package com.meetingai.backend.crm;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.meetingai.backend.crypto.EncryptionService;
import com.meetingai.backend.meeting.Meeting;
import com.meetingai.backend.meeting.MeetingAccessService;
import com.meetingai.backend.meeting.MeetingRepository;
import com.meetingai.backend.summary.Summary;
import com.meetingai.backend.summary.SummaryNotApprovedException;
import com.meetingai.backend.summary.SummaryNotFoundException;
import com.meetingai.backend.summary.SummaryRepository;
import com.meetingai.backend.user.User;

import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CrmSyncServiceTest {

	@Mock
	private MeetingAccessService meetingAccessService;

	@Mock
	private MeetingRepository meetingRepository;

	@Mock
	private SummaryRepository summaryRepository;

	@Mock
	private CrmConnectionRepository crmConnectionRepository;

	@Mock
	private EncryptionService encryptionService;

	@Mock
	private PipedriveClient pipedriveClient;

	private CrmSyncService crmSyncService;
	private User user;
	private Meeting meeting;
	private UUID meetingId;

	@BeforeEach
	void setUp() {
		crmSyncService = new CrmSyncService(meetingAccessService, meetingRepository, summaryRepository,
				crmConnectionRepository, encryptionService, pipedriveClient, JsonMapper.builder().build());
		user = new User("google-sub-1", "dev@meetingai.com", "Dev User", null);
		ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
		meeting = new Meeting(user, "Reunião de vendas", "reuniao.mp3", "user-1/key.mp3");
		meetingId = UUID.randomUUID();
		ReflectionTestUtils.setField(meeting, "id", meetingId);
		given(meetingAccessService.getOwnedMeeting(user, meetingId)).willReturn(meeting);
	}

	private static final String APPROVED_JSON = "{\"summary\":\"resumo\",\"decisions\":[\"fechar\"],\"nextSteps\":[],"
			+ "\"mentionedValues\":[],\"paymentMethod\":\"boleto\",\"objections\":[]}";

	@Test
	void sendsApprovedSummaryCreatesDealAddsNoteAndMarksMeetingSent() {
		Summary summary = new Summary(meeting, APPROVED_JSON);
		summary.approve();
		given(summaryRepository.findByMeetingId(meetingId)).willReturn(Optional.of(summary));
		CrmConnection connection = new CrmConnection(user, CrmProvider.PIPEDRIVE, "encrypted-access", null, null);
		given(crmConnectionRepository.findByUserId(user.getId())).willReturn(Optional.of(connection));
		given(encryptionService.decrypt("encrypted-access")).willReturn("plain-access");
		given(pipedriveClient.createDeal("plain-access", "Reunião de vendas")).willReturn(42L);

		crmSyncService.sendApprovedSummaryToCrm(user, meetingId);

		verify(pipedriveClient).addNote(eq("plain-access"), eq(42L), anyString());
		verify(meetingRepository).save(meeting);
		assertThat(meeting.getStatus().name()).isEqualTo("SENT_TO_CRM");
	}

	@Test
	void throwsWhenNoSummaryExists() {
		given(summaryRepository.findByMeetingId(meetingId)).willReturn(Optional.empty());

		assertThatThrownBy(() -> crmSyncService.sendApprovedSummaryToCrm(user, meetingId))
				.isInstanceOf(SummaryNotFoundException.class);
	}

	@Test
	void throwsWhenSummaryNotApproved() {
		Summary summary = new Summary(meeting, APPROVED_JSON);
		given(summaryRepository.findByMeetingId(meetingId)).willReturn(Optional.of(summary));

		assertThatThrownBy(() -> crmSyncService.sendApprovedSummaryToCrm(user, meetingId))
				.isInstanceOf(SummaryNotApprovedException.class);
		verify(pipedriveClient, never()).createDeal(anyString(), anyString());
	}

	@Test
	void throwsWhenNoCrmConnectionConfigured() {
		Summary summary = new Summary(meeting, APPROVED_JSON);
		summary.approve();
		given(summaryRepository.findByMeetingId(meetingId)).willReturn(Optional.of(summary));
		given(crmConnectionRepository.findByUserId(user.getId())).willReturn(Optional.empty());

		assertThatThrownBy(() -> crmSyncService.sendApprovedSummaryToCrm(user, meetingId))
				.isInstanceOf(CrmConnectionException.class);
		verify(pipedriveClient, never()).createDeal(anyString(), anyString());
	}

}
