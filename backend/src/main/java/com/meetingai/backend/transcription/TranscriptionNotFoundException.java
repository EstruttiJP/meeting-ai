package com.meetingai.backend.transcription;

import java.util.UUID;

public class TranscriptionNotFoundException extends RuntimeException {

	public TranscriptionNotFoundException(UUID meetingId) {
		super("Transcrição ainda não disponível para a reunião: " + meetingId);
	}

}
