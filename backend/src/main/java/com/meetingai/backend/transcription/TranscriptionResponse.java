package com.meetingai.backend.transcription;

import java.time.Instant;
import java.util.UUID;

public record TranscriptionResponse(
		UUID id,
		UUID meetingId,
		String content,
		String language,
		String provider,
		Instant createdAt) {

	public static TranscriptionResponse from(Transcription transcription) {
		return new TranscriptionResponse(
				transcription.getId(),
				transcription.getMeeting().getId(),
				transcription.getContent(),
				transcription.getLanguage(),
				transcription.getProvider(),
				transcription.getCreatedAt());
	}

}
