package com.meetingai.backend.meeting;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.meetingai.backend.storage.StorageService;
import com.meetingai.backend.usagequota.UsageQuotaExceededException;
import com.meetingai.backend.usagequota.UsageQuotaService;
import com.meetingai.backend.user.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class MeetingUploadServiceTest {

	private static final long MAX_FILE_SIZE_MB = 1;
	private static final long RETENTION_DAYS = 7;

	@Mock
	private MeetingRepository meetingRepository;

	@Mock
	private UsageQuotaService usageQuotaService;

	@Mock
	private StorageService storageService;

	private MeetingUploadService meetingUploadService;
	private User user;

	@BeforeEach
	void setUp() {
		meetingUploadService = new MeetingUploadService(meetingRepository, usageQuotaService, storageService,
				MAX_FILE_SIZE_MB, RETENTION_DAYS);
		user = new User("google-sub-1", "dev@meetingai.com", "Dev User", null);
	}

	@Test
	void acceptsValidUploadAndPersistsMeeting() {
		MockMultipartFile file = new MockMultipartFile("file", "reuniao.mp3", "audio/mpeg", "conteudo".getBytes());
		given(meetingRepository.save(any(Meeting.class))).willAnswer(invocation -> invocation.getArgument(0));

		Meeting meeting = meetingUploadService.upload(user, "Reunião de vendas", file);

		assertThat(meeting.getStatus()).isEqualTo(MeetingStatus.UPLOADED);
		assertThat(meeting.getOriginalFilename()).isEqualTo("reuniao.mp3");
		assertThat(meeting.getStorageKey()).contains("reuniao.mp3");
		assertThat(meeting.getExpiresAt())
				.isAfter(java.time.Instant.now().plus(RETENTION_DAYS - 1, java.time.temporal.ChronoUnit.DAYS))
				.isBefore(java.time.Instant.now().plus(RETENTION_DAYS + 1, java.time.temporal.ChronoUnit.DAYS));
		verify(usageQuotaService).consumeUploadSlot(user);
		verify(storageService).store(anyString(), any(), anyLong(), anyString());
	}

	@Test
	void rejectsUnsupportedFormatBeforeCheckingQuotaOrStoring() {
		MockMultipartFile file = new MockMultipartFile("file", "reuniao.txt", "text/plain", "conteudo".getBytes());

		assertThatThrownBy(() -> meetingUploadService.upload(user, "Reunião de vendas", file))
				.isInstanceOf(UnsupportedMeetingFormatException.class);
		verifyNoInteractions(usageQuotaService, storageService);
	}

	@Test
	void rejectsFileLargerThanConfiguredLimit() {
		byte[] tooLarge = new byte[(int) (MAX_FILE_SIZE_MB * 1024 * 1024) + 1];
		MockMultipartFile file = new MockMultipartFile("file", "reuniao.mp3", "audio/mpeg", tooLarge);

		assertThatThrownBy(() -> meetingUploadService.upload(user, "Reunião de vendas", file))
				.isInstanceOf(MeetingFileTooLargeException.class);
		verifyNoInteractions(usageQuotaService, storageService);
	}

	@Test
	void propagatesQuotaExceededWithoutStoringFile() {
		MockMultipartFile file = new MockMultipartFile("file", "reuniao.mp3", "audio/mpeg", "conteudo".getBytes());
		willThrow(new UsageQuotaExceededException(10)).given(usageQuotaService).consumeUploadSlot(user);

		assertThatThrownBy(() -> meetingUploadService.upload(user, "Reunião de vendas", file))
				.isInstanceOf(UsageQuotaExceededException.class);
		verify(storageService, never()).store(anyString(), any(), anyLong(), anyString());
		verify(meetingRepository, never()).save(any());
	}

}
