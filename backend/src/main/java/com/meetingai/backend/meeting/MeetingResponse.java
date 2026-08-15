package com.meetingai.backend.meeting;

import java.time.Instant;
import java.util.UUID;

public record MeetingResponse(
		UUID id,
		String title,
		String originalFilename,
		MeetingStatus status,
		Instant uploadedAt,
		Instant expiresAt,
		Instant sentToCrmAt,
		MeetingFailureCategory failureCategory) {

	// failureReason fica de fora de propósito: é mensagem técnica (às vezes com
	// trecho da resposta bruta do modelo) para log e diagnóstico, não para o
	// usuário final — o frontend monta o texto amigável a partir da categoria.
	public static MeetingResponse from(Meeting meeting) {
		return new MeetingResponse(
				meeting.getId(),
				meeting.getTitle(),
				meeting.getOriginalFilename(),
				meeting.getStatus(),
				meeting.getUploadedAt(),
				meeting.getExpiresAt(),
				meeting.getSentToCrmAt(),
				meeting.getFailureCategory());
	}

}
