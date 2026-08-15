package com.meetingai.backend.meeting;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetingai.backend.summary.Summary;
import com.meetingai.backend.summary.SummaryContent;
import com.meetingai.backend.summary.SummaryRepository;
import com.meetingai.backend.transcription.Transcription;
import com.meetingai.backend.transcription.TranscriptionRepository;
import com.meetingai.backend.transcription.TranscriptionResult;

import tools.jackson.databind.ObjectMapper;

/**
 * Passos transacionais do pipeline (uma transação curta por passo, não uma
 * só cobrindo as chamadas de rede pro Whisper/LLM). Fica numa classe própria
 * porque {@link MeetingPipelineService#process} chama esses métodos de fora
 * — self-invocation dentro da mesma classe faria o proxy de @Transactional
 * do Spring ser ignorado silenciosamente.
 */
@Service
class MeetingPipelineSteps {

	private final MeetingRepository meetingRepository;
	private final TranscriptionRepository transcriptionRepository;
	private final SummaryRepository summaryRepository;
	private final ObjectMapper objectMapper;

	MeetingPipelineSteps(MeetingRepository meetingRepository,
			TranscriptionRepository transcriptionRepository,
			SummaryRepository summaryRepository,
			ObjectMapper objectMapper) {
		this.meetingRepository = meetingRepository;
		this.transcriptionRepository = transcriptionRepository;
		this.summaryRepository = summaryRepository;
		this.objectMapper = objectMapper;
	}

	@Transactional
	PipelineContext markTranscribing(UUID meetingId) {
		Meeting meeting = getMeetingOrThrow(meetingId);
		meeting.markTranscribing();
		meetingRepository.save(meeting);
		return new PipelineContext(meeting.getStorageKey(), meeting.getOriginalFilename(), meeting.getUser().getId());
	}

	@Transactional
	void saveTranscriptionAndMarkSummarizing(UUID meetingId, TranscriptionResult result, String providerName) {
		Meeting meetingRef = meetingRepository.getReferenceById(meetingId);
		transcriptionRepository.save(new Transcription(meetingRef, result.content(), result.language(), providerName));

		Meeting meeting = getMeetingOrThrow(meetingId);
		meeting.markSummarizing();
		meetingRepository.save(meeting);
	}

	@Transactional
	void saveSummaryAndMarkReady(UUID meetingId, SummaryContent summaryContent) {
		Meeting meetingRef = meetingRepository.getReferenceById(meetingId);
		summaryRepository.save(new Summary(meetingRef, objectMapper.writeValueAsString(summaryContent)));

		Meeting meeting = getMeetingOrThrow(meetingId);
		meeting.markReady();
		meetingRepository.save(meeting);
	}

	@Transactional
	void markFailed(UUID meetingId, MeetingFailureCategory category, String reason) {
		meetingRepository.findById(meetingId).ifPresent(meeting -> {
			meeting.markFailed(category, reason);
			meetingRepository.save(meeting);
		});
	}

	private Meeting getMeetingOrThrow(UUID meetingId) {
		return meetingRepository.findById(meetingId)
				.orElseThrow(() -> new IllegalStateException("Meeting não encontrada: " + meetingId));
	}

	record PipelineContext(String storageKey, String originalFilename, UUID userId) {
	}

}
