package com.meetingai.backend.crm;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CrmConnectionRequest(
		@NotNull CrmProvider provider,
		@NotBlank String authorizationCode) {
}
