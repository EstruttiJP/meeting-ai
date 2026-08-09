package com.meetingai.backend.meeting;

public class UnsupportedMeetingFormatException extends RuntimeException {

	public UnsupportedMeetingFormatException(String originalFilename) {
		super("Formato de arquivo não suportado: %s. Formatos aceitos: mp3, mp4, wav, m4a."
				.formatted(originalFilename));
	}

}
