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
		String json = "{\"summary\":\"resumo salvo\",\"decisions\":[],\"nextSteps\":[],\"mentionedValues\":[],\"paymentMethod\":null,\"objections\":[]}";
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
		String originalJson = "{\"summary\":\"original\",\"decisions\":[],\"nextSteps\":[],\"mentionedValues\":[],\"paymentMethod\":null,\"objections\":[]}";
		Summary summary = new Summary(meeting, originalJson);
		given(meetingRepository.findById(meetingId)).willReturn(Optional.of(meeting));
		given(summaryRepository.findByMeetingId(meetingId)).willReturn(Optional.of(summary));
		given(summaryRepository.save(any(Summary.class))).willAnswer(invocation -> invocation.getArgument(0));
		SummaryContent editedContent = new SummaryContent("resumo editado", List.of("decisão"), List.of(), List.of(), "boleto", List.of());

		SummaryResponse response = summaryService.approve(user, meetingId, editedContent);

		assertThat(response.content().summary()).isEqualTo("resumo editado");
		assertThat(response.approved()).isTrue();
		assertThat(response.approvedAt()).isNotNull();
		assertThat(summary.isApproved()).isTrue();
	}

	@Test
	void approveThrowsWhenMeetingBelongsToAnotherUser() {
		User otherUser = new User("google-sub-2", "other@meetingai.com", "Other User", null);
		ReflectionTestUtils.setField(otherUser, "id", UUID.randomUUID());
		Meeting othersMeeting = new Meeting(otherUser, "Reunião de outro usuário", "r.mp3", "key.mp3");
		given(meetingRepository.findById(meetingId)).willReturn(Optional.of(othersMeeting));
		SummaryContent editedContent = new SummaryContent("resumo editado", List.of(), List.of(), List.of(), null, List.of());

		assertThatThrownBy(() -> summaryService.approve(user, meetingId, editedContent))
				.isInstanceOf(MeetingNotFoundException.class);
	}

}
