package com.meetingai.backend.meeting;

public class MeetingFileTooLargeException extends RuntimeException {

	public MeetingFileTooLargeException(long maxFileSizeBytes) {
		super("Arquivo excede o tamanho máximo permitido de %dMB.".formatted(maxFileSizeBytes / (1024 * 1024)));
	}

}
