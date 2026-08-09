package com.meetingai.backend.meeting;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.meetingai.backend.storage.StorageException;
import com.meetingai.backend.storage.StorageService;
import com.meetingai.backend.user.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MeetingExpirationJobTest {

	@Mock
	private MeetingRepository meetingRepository;

	@Mock
	private StorageService storageService;

	private MeetingExpirationJob job;

	@BeforeEach
	void setUp() {
		job = new MeetingExpirationJob(meetingRepository, storageService);
	}

	@Test
	void expiresMeetingsPastRetentionAndDeletesTheirFiles() {
		User user = new User("google-sub-1", "dev@meetingai.com", "Dev User", null);
		Meeting overdue = new Meeting(user, "Reunião antiga", "reuniao.mp3", "user-1/key.mp3");
		overdue.scheduleExpiration(Instant.now().minus(1, ChronoUnit.DAYS));
		given(meetingRepository.findByStatusNotAndExpiresAtBefore(eq(MeetingStatus.EXPIRED), any(Instant.class)))
				.willReturn(List.of(overdue));

		job.expireOldMeetings();

		verify(storageService).delete("user-1/key.mp3");
		verify(meetingRepository).save(overdue);
		assertThat(overdue.getStatus()).isEqualTo(MeetingStatus.EXPIRED);
	}

	@Test
	void stillMarksExpiredWhenFileDeletionFails() {
		User user = new User("google-sub-1", "dev@meetingai.com", "Dev User", null);
		Meeting overdue = new Meeting(user, "Reunião antiga", "reuniao.mp3", "user-1/key.mp3");
		overdue.scheduleExpiration(Instant.now().minus(1, ChronoUnit.DAYS));
		given(meetingRepository.findByStatusNotAndExpiresAtBefore(eq(MeetingStatus.EXPIRED), any(Instant.class)))
				.willReturn(List.of(overdue));
		willThrow(new StorageException("arquivo não encontrado")).given(storageService).delete("user-1/key.mp3");

		job.expireOldMeetings();

		assertThat(overdue.getStatus()).isEqualTo(MeetingStatus.EXPIRED);
		verify(meetingRepository).save(overdue);
	}

	@Test
	void doesNothingWhenNoMeetingsAreOverdue() {
		given(meetingRepository.findByStatusNotAndExpiresAtBefore(eq(MeetingStatus.EXPIRED), any(Instant.class)))
				.willReturn(List.of());

		job.expireOldMeetings();

		verify(meetingRepository, org.mockito.Mockito.never()).save(any());
	}

}
