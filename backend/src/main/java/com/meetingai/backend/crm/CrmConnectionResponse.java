package com.meetingai.backend.crm;

import java.time.Instant;
import java.util.UUID;

public record CrmConnectionResponse(
		UUID id,
		CrmProvider provider,
		Instant connectedAt,
		Instant tokenExpiresAt) {

	public static CrmConnectionResponse from(CrmConnection connection) {
		return new CrmConnectionResponse(
				connection.getId(),
				connection.getProvider(),
				connection.getConnectedAt(),
				connection.getTokenExpiresAt());
	}

}
