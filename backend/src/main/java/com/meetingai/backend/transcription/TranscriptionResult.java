package com.meetingai.backend.transcription;

import java.util.List;

public record TranscriptionResult(String content, String language, List<TranscriptionSegment> segments) {

	public TranscriptionResult {
		segments = segments == null ? List.of() : List.copyOf(segments);
	}

}
