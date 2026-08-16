package com.meetingai.backend.meeting;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.meetingai.backend.meeting.MeetingPipelineSteps.PipelineContext;
import com.meetingai.backend.storage.StorageException;
import com.meetingai.backend.storage.StorageService;
import com.meetingai.backend.summary.SummaryContent;
import com.meetingai.backend.summary.SummaryFormatException;
import com.meetingai.backend.summary.SummaryGenerationException;
import com.meetingai.backend.summary.SummaryOrchestrator;
import com.meetingai.backend.transcription.TranscriptionException;
import com.meetingai.backend.transcription.TranscriptionProvider;
import com.meetingai.backend.transcription.TranscriptionResult;
import com.meetingai.backend.user.User;
import com.meetingai.backend.user.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MeetingPipelineServiceTest {

	@Mock
	private MeetingPipelineSteps steps;

	@Mock
	private StorageService storageService;

	@Mock
	private TranscriptionProvider transcriptionProvider;

	@Mock
	private SummaryOrchestrator summaryOrchestrator;

	@Mock
	private UserRepository userRepository;

	@Captor
	private ArgumentCaptor<String> reasonCaptor;

	private static final TranscriptionResult TRANSCRICAO =
			new TranscriptionResult("transcrição da reunião", "pt", List.of());

	private MeetingPipelineService pipelineService;
	private UUID meetingId;
	private UUID userId;
	private User user;
	private PipelineContext context;

	@BeforeEach
	void setUp() {
		pipelineService = new MeetingPipelineService(steps, storageService, transcriptionProvider, summaryOrchestrator, userRepository);
		meetingId = UUID.randomUUID();
		userId = UUID.randomUUID();
		user = new User("google-sub-1", "dev@meetingai.com", "Dev User", null);
		context = new PipelineContext("user-1/key-reuniao.mp3", "reuniao.mp3", userId, MeetingType.DAILY);
	}

	@Test
	void happyPathTranscribesAndSummarizesThenMarksReady() {
		given(steps.markTranscribing(meetingId)).willReturn(context);
		given(storageService.retrieve(context.storageKey())).willReturn(new ByteArrayInputStream("audio".getBytes()));
		given(transcriptionProvider.transcribe(any(), eq("reuniao.mp3"), anyString())).willReturn(TRANSCRICAO);
		given(transcriptionProvider.providerName()).willReturn("whisper-local");
		given(userRepository.findById(userId)).willReturn(Optional.of(user));
		SummaryContent summaryContent = new SummaryContent("resumo", List.of());
		given(summaryOrchestrator.summarize(user, TRANSCRICAO, MeetingType.DAILY)).willReturn(summaryContent);

		pipelineService.process(meetingId);

		verify(steps).saveTranscriptionAndMarkSummarizing(meetingId, TRANSCRICAO, "whisper-local");
		verify(steps).saveSummaryAndMarkReady(meetingId, summaryContent);
		verify(steps, never()).markFailed(any(), any(), anyString());
	}

	@Test
	void marksFailedWithTranscriptionCategoryAndTechnicalReason() {
		given(steps.markTranscribing(meetingId)).willReturn(context);
		given(storageService.retrieve(context.storageKey())).willReturn(new ByteArrayInputStream("audio".getBytes()));
		willThrow(new TranscriptionException("422 audio_file: Field required"))
				.given(transcriptionProvider).transcribe(any(), eq("reuniao.mp3"), anyString());

		pipelineService.process(meetingId);

		verify(steps).markFailed(eq(meetingId), eq(MeetingFailureCategory.TRANSCRIPTION),
				reasonCaptor.capture());
		assertThat(reasonCaptor.getValue()).contains("422 audio_file: Field required");
		verify(steps, never()).saveTranscriptionAndMarkSummarizing(any(), any(), anyString());
	}

	@Test
	void marksFailedWithUnknownCategoryWhenStorageRetrievalFails() {
		given(steps.markTranscribing(meetingId)).willReturn(context);
		willThrow(new StorageException("arquivo não encontrado")).given(storageService).retrieve(context.storageKey());

		pipelineService.process(meetingId);

		verify(steps).markFailed(eq(meetingId), eq(MeetingFailureCategory.UNKNOWN), reasonCaptor.capture());
		assertThat(reasonCaptor.getValue()).contains("arquivo não encontrado");
	}

	@Test
	void marksFailedWithSummaryCategoryWhenProviderCallFails() {
		givenTranscriptionSucceeded();
		willThrow(new SummaryGenerationException("Falha ao chamar a API de IA",
				new IllegalStateException("402 Payment Required")))
				.given(summaryOrchestrator).summarize(user, TRANSCRICAO, MeetingType.DAILY);

		pipelineService.process(meetingId);

		verify(steps).markFailed(eq(meetingId), eq(MeetingFailureCategory.SUMMARY), reasonCaptor.capture());
		assertThat(reasonCaptor.getValue())
				.contains("Falha ao chamar a API de IA")
				.contains("402 Payment Required");
		verify(steps, never()).saveSummaryAndMarkReady(any(), any());
	}

	@Test
	void marksFailedWithInvalidFormatCategoryWhenModelBreaksTheSchema() {
		givenTranscriptionSucceeded();
		willThrow(new SummaryFormatException("Resposta bruta: desculpe, não consigo ajudar"))
				.given(summaryOrchestrator).summarize(user, TRANSCRICAO, MeetingType.DAILY);

		pipelineService.process(meetingId);

		verify(steps).markFailed(eq(meetingId), eq(MeetingFailureCategory.INVALID_SUMMARY_FORMAT),
				reasonCaptor.capture());
		assertThat(reasonCaptor.getValue()).contains("desculpe, não consigo ajudar");
	}

	private void givenTranscriptionSucceeded() {
		given(steps.markTranscribing(meetingId)).willReturn(context);
		given(storageService.retrieve(context.storageKey())).willReturn(new ByteArrayInputStream("audio".getBytes()));
		given(transcriptionProvider.transcribe(any(), eq("reuniao.mp3"), anyString()))
				.willReturn(TRANSCRICAO);
		given(transcriptionProvider.providerName()).willReturn("whisper-local");
		given(userRepository.findById(userId)).willReturn(Optional.of(user));
	}

}
