package com.meetingai.backend.summary;

import java.util.UUID;

public class SummaryNotApprovedException extends RuntimeException {

	public SummaryNotApprovedException(UUID meetingId) {
		super("Resumo da reunião " + meetingId + " ainda não foi aprovado — edite e aprove antes de enviar ao CRM.");
	}

}
