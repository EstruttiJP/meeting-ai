package com.meetingai.backend.aiprovider;

import java.time.Instant;
import java.util.UUID;

public record AiProviderConfigResponse(
		UUID id,
		AiProvider provider,
		boolean hasApiKey,
		boolean isDefault,
		Instant createdAt) {

	public static AiProviderConfigResponse from(AiProviderConfig config) {
		return new AiProviderConfigResponse(
				config.getId(),
				config.getProvider(),
				config.getApiKeyEncrypted() != null,
				config.isDefault(),
				config.getCreatedAt());
	}

}
