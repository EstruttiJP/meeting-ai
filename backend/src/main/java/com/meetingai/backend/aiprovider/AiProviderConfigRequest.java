package com.meetingai.backend.aiprovider;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AiProviderConfigRequest(
		@NotNull AiProvider provider,
		@Size(max = 2048) String apiKey,
		boolean isDefault) {
}
