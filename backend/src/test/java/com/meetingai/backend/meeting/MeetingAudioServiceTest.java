package com.meetingai.backend.meeting;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.meetingai.backend.meeting.MeetingAudioService.MeetingAudio;
import com.meetingai.backend.storage.StorageException;
import com.meetingai.backend.storage.StorageService;
import com.meetingai.backend.user.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MeetingAudioServiceTest {

	@Mock
	private MeetingAccessService meetingAccessService;

	@Mock
	private StorageService storageService;

	private MeetingAudioService audioService;
	private User user;
	private Meeting meeting;
	private UUID meetingId;

	@BeforeEach
	void setUp() {
		audioService = new MeetingAudioService(meetingAccessService, storageService);
		user = new User("google-sub-1", "dev@meetingai.com", "Dev User", null);
		meeting = new Meeting(user, "Reunião de vendas", "reuniao.mp3", "user-1/key.mp3");
		meetingId = UUID.randomUUID();
		ReflectionTestUtils.setField(meeting, "id", meetingId);
	}

	@Test
	void servesTheOriginalAudioWithTheContentTypeOfTheUploadedFile() {
		meeting.scheduleExpiration(Instant.now().plus(Duration.ofDays(7)));
		given(meetingAccessService.getOwnedMeeting(user, meetingId)).willReturn(meeting);
		given(storageService.retrieve("user-1/key.mp3")).willReturn(new ByteArrayInputStream("audio".getBytes()));

		MeetingAudio audio = audioService.load(user, meetingId);

		assertThat(audio.contentType()).isEqualTo("audio/mpeg");
		assertThat(audio.filename()).isEqualTo("reuniao.mp3");
	}

	@Test
	void refusesToServeAudioOfAnExpiredMeeting() {
		meeting.markExpired();
		given(meetingAccessService.getOwnedMeeting(user, meetingId)).willReturn(meeting);

		assertThatThrownBy(() -> audioService.load(user, meetingId))
				.isInstanceOf(MeetingAudioUnavailableException.class)
				.hasMessageContaining("período de retenção");

		verify(storageService, never()).retrieve(org.mockito.ArgumentMatchers.anyString());
	}

	@Test
	void refusesToServeAudioWhoseRetentionWindowClosedBeforeTheExpirationJobRan() {
		// expires_at já venceu mas o job ainda não rodou: o status continua READY
		// e mesmo assim o áudio não deve ser servido.
		meeting.scheduleExpiration(Instant.now().minus(Duration.ofMinutes(1)));
		given(meetingAccessService.getOwnedMeeting(user, meetingId)).willReturn(meeting);

		assertThatThrownBy(() -> audioService.load(user, meetingId))
				.isInstanceOf(MeetingAudioUnavailableException.class);

		verify(storageService, never()).retrieve(org.mockito.ArgumentMatchers.anyString());
	}

	@Test
	void reportsAudioAsUnavailableWhenTheFileVanishedFromStorage() {
		given(meetingAccessService.getOwnedMeeting(user, meetingId)).willReturn(meeting);
		willThrow(new StorageException("arquivo não encontrado")).given(storageService).retrieve("user-1/key.mp3");

		assertThatThrownBy(() -> audioService.load(user, meetingId))
				.isInstanceOf(MeetingAudioUnavailableException.class);
	}

	@Test
	void doesNotServeAudioOfAMeetingTheUserDoesNotOwn() {
		willThrow(new MeetingNotFoundException(meetingId))
				.given(meetingAccessService).getOwnedMeeting(user, meetingId);

		assertThatThrownBy(() -> audioService.load(user, meetingId))
				.isInstanceOf(MeetingNotFoundException.class);
	}

}
