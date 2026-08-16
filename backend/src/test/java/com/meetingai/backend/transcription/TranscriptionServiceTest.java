package com.meetingai.backend.transcription;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.meetingai.backend.meeting.Meeting;
import com.meetingai.backend.meeting.MeetingAccessService;
import com.meetingai.backend.user.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class TranscriptionServiceTest {

	@Mock
	private MeetingAccessService meetingAccessService;

	@Mock
	private TranscriptionRepository transcriptionRepository;

	private TranscriptionService transcriptionService;
	private User user;
	private Meeting meeting;
	private UUID meetingId;

	@BeforeEach
	void setUp() {
		transcriptionService = new TranscriptionService(meetingAccessService, transcriptionRepository);
		user = new User("google-sub-1", "dev@meetingai.com", "Dev User", null);
		meeting = new Meeting(user, "Reunião de vendas", "reuniao.mp3", "user-1/key.mp3");
		meetingId = UUID.randomUUID();
		ReflectionTestUtils.setField(meeting, "id", meetingId);
	}

	@Test
	void returnsTranscriptionWhenMeetingIsOwnedAndTranscriptionExists() {
		Transcription transcription = new Transcription(meeting, "conteúdo", "pt", "whisper-local", null);
		given(meetingAccessService.getOwnedMeeting(user, meetingId)).willReturn(meeting);
		given(transcriptionRepository.findByMeetingId(meetingId)).willReturn(Optional.of(transcription));

		TranscriptionResponse response = transcriptionService.getForMeeting(user, meetingId);

		assertThat(response.content()).isEqualTo("conteúdo");
		assertThat(response.provider()).isEqualTo("whisper-local");
	}

	@Test
	void throwsWhenTranscriptionNotYetAvailable() {
		given(meetingAccessService.getOwnedMeeting(user, meetingId)).willReturn(meeting);
		given(transcriptionRepository.findByMeetingId(meetingId)).willReturn(Optional.empty());

		assertThatThrownBy(() -> transcriptionService.getForMeeting(user, meetingId))
				.isInstanceOf(TranscriptionNotFoundException.class);
	}

}
