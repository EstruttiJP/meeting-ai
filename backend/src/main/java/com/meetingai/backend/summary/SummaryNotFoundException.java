package com.meetingai.backend.summary;

import java.util.UUID;

public class SummaryNotFoundException extends RuntimeException {

	public SummaryNotFoundException(UUID meetingId) {
		super("Resumo ainda não disponível para a reunião: " + meetingId);
	}

}
