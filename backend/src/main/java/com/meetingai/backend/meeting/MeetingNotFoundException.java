package com.meetingai.backend.meeting;

import java.util.UUID;

public class MeetingNotFoundException extends RuntimeException {

	public MeetingNotFoundException(UUID meetingId) {
		super("Reunião não encontrada: " + meetingId);
	}

}
