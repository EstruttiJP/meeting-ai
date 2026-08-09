package com.meetingai.backend.meeting;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.meetingai.backend.meeting.MeetingPipelineSteps.PipelineContext;
import com.meetingai.backend.storage.StorageException;
import com.meetingai.backend.storage.StorageService;
import com.meetingai.backend.summary.SummaryContent;
import com.meetingai.backend.summary.SummaryGenerationException;
import com.meetingai.backend.summary.SummaryOrchestrator;
import com.meetingai.backend.transcription.TranscriptionException;
import com.meetingai.backend.transcription.TranscriptionProvider;
import com.meetingai.backend.transcription.TranscriptionResult;
import com.meetingai.backend.user.User;
import com.meetingai.backend.user.UserRepository;

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
		context = new PipelineContext("user-1/key-reuniao.mp3", "reuniao.mp3", userId);
	}

	@Test
	void happyPathTranscribesAndSummarizesThenMarksReady() {
		given(steps.markTranscribing(meetingId)).willReturn(context);
		given(storageService.retrieve(context.storageKey())).willReturn(new ByteArrayInputStream("audio".getBytes()));
		TranscriptionResult transcriptionResult = new TranscriptionResult("transcrição da reunião", "pt");
		given(transcriptionProvider.transcribe(any(), eq("reuniao.mp3"), anyString())).willReturn(transcriptionResult);
		given(transcriptionProvider.providerName()).willReturn("whisper-local");
		given(userRepository.findById(userId)).willReturn(Optional.of(user));
		SummaryContent summaryContent = new SummaryContent("resumo", List.of(), List.of(), List.of(), null, List.of());
		given(summaryOrchestrator.summarize(user, "transcrição da reunião")).willReturn(summaryContent);

		pipelineService.process(meetingId);

		verify(steps).saveTranscriptionAndMarkSummarizing(meetingId, transcriptionResult, "whisper-local");
		verify(steps).saveSummaryAndMarkReady(meetingId, summaryContent);
		verify(steps, never()).markFailed(any());
	}

	@Test
	void marksFailedWhenTranscriptionFails() {
		given(steps.markTranscribing(meetingId)).willReturn(context);
		given(storageService.retrieve(context.storageKey())).willReturn(new ByteArrayInputStream("audio".getBytes()));
		willThrow(new TranscriptionException("falha no whisper"))
				.given(transcriptionProvider).transcribe(any(), eq("reuniao.mp3"), anyString());

		pipelineService.process(meetingId);

		verify(steps).markFailed(meetingId);
		verify(steps, never()).saveTranscriptionAndMarkSummarizing(any(), any(), anyString());
	}

	@Test
	void marksFailedWhenStorageRetrievalFails() {
		given(steps.markTranscribing(meetingId)).willReturn(context);
		willThrow(new StorageException("arquivo não encontrado")).given(storageService).retrieve(context.storageKey());

		pipelineService.process(meetingId);

		verify(steps).markFailed(meetingId);
	}

	@Test
	void marksFailedWhenSummaryGenerationFails() {
		given(steps.markTranscribing(meetingId)).willReturn(context);
		given(storageService.retrieve(context.storageKey())).willReturn(new ByteArrayInputStream("audio".getBytes()));
		TranscriptionResult transcriptionResult = new TranscriptionResult("transcrição da reunião", "pt");
		given(transcriptionProvider.transcribe(any(), eq("reuniao.mp3"), anyString())).willReturn(transcriptionResult);
		given(transcriptionProvider.providerName()).willReturn("whisper-local");
		given(userRepository.findById(userId)).willReturn(Optional.of(user));
		willThrow(new SummaryGenerationException("resposta fora do schema"))
				.given(summaryOrchestrator).summarize(user, "transcrição da reunião");

		pipelineService.process(meetingId);

		verify(steps).markFailed(meetingId);
		verify(steps, never()).saveSummaryAndMarkReady(any(), any());
	}

}
