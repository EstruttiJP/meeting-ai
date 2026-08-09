package com.meetingai.backend.meeting;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.meetingai.backend.meeting.MeetingPipelineSteps.PipelineContext;
import com.meetingai.backend.summary.Summary;
import com.meetingai.backend.summary.SummaryContent;
import com.meetingai.backend.summary.SummaryRepository;
import com.meetingai.backend.transcription.Transcription;
import com.meetingai.backend.transcription.TranscriptionRepository;
import com.meetingai.backend.transcription.TranscriptionResult;
import com.meetingai.backend.user.User;

import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeetingPipelineStepsTest {

	@Mock
	private MeetingRepository meetingRepository;

	@Mock
	private TranscriptionRepository transcriptionRepository;

	@Mock
	private SummaryRepository summaryRepository;

	private MeetingPipelineSteps steps;
	private Meeting meeting;
	private UUID meetingId;

	@BeforeEach
	void setUp() {
		steps = new MeetingPipelineSteps(meetingRepository, transcriptionRepository, summaryRepository, JsonMapper.builder().build());
		User user = new User("google-sub-1", "dev@meetingai.com", "Dev User", null);
		meeting = new Meeting(user, "Reunião de vendas", "reuniao.mp3", "user-1/key-reuniao.mp3");
		meetingId = UUID.randomUUID();
	}

	@Test
	void markTranscribingUpdatesStatusAndReturnsPipelineContext() {
		given(meetingRepository.findById(meetingId)).willReturn(Optional.of(meeting));

		PipelineContext context = steps.markTranscribing(meetingId);

		assertThat(meeting.getStatus()).isEqualTo(MeetingStatus.TRANSCRIBING);
		assertThat(context.storageKey()).isEqualTo("user-1/key-reuniao.mp3");
		assertThat(context.originalFilename()).isEqualTo("reuniao.mp3");
		assertThat(context.userId()).isEqualTo(meeting.getUser().getId());
		verify(meetingRepository).save(meeting);
	}

	@Test
	void saveTranscriptionAndMarkSummarizingPersistsTranscriptionAndUpdatesStatus() {
		when(meetingRepository.getReferenceById(meetingId)).thenReturn(meeting);
		given(meetingRepository.findById(meetingId)).willReturn(Optional.of(meeting));
		TranscriptionResult result = new TranscriptionResult("transcrição completa", "pt");

		steps.saveTranscriptionAndMarkSummarizing(meetingId, result, "whisper-local");

		verify(transcriptionRepository).save(argThat((Transcription t) ->
				t.getContent().equals("transcrição completa") && t.getProvider().equals("whisper-local")));
		assertThat(meeting.getStatus()).isEqualTo(MeetingStatus.SUMMARIZING);
	}

	@Test
	void saveSummaryAndMarkReadyPersistsSummaryJsonAndMarksReady() {
		when(meetingRepository.getReferenceById(meetingId)).thenReturn(meeting);
		given(meetingRepository.findById(meetingId)).willReturn(Optional.of(meeting));
		SummaryContent content = new SummaryContent("resumo objetivo", List.of("decisão 1"), List.of(), List.of(), null, List.of());

		steps.saveSummaryAndMarkReady(meetingId, content);

		verify(summaryRepository).save(argThat((Summary s) -> s.getContent().contains("resumo objetivo")));
		assertThat(meeting.getStatus()).isEqualTo(MeetingStatus.READY);
	}

	@Test
	void markFailedUpdatesStatusWhenMeetingExists() {
		given(meetingRepository.findById(meetingId)).willReturn(Optional.of(meeting));

		steps.markFailed(meetingId);

		assertThat(meeting.getStatus()).isEqualTo(MeetingStatus.FAILED);
		verify(meetingRepository).save(meeting);
	}

	@Test
	void markFailedDoesNothingWhenMeetingIsMissing() {
		given(meetingRepository.findById(meetingId)).willReturn(Optional.empty());

		steps.markFailed(meetingId);

		verify(meetingRepository, never()).save(any());
	}

}
