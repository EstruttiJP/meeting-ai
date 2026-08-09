package com.meetingai.backend.summary;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record SummaryUpdateRequest(
		@NotNull @Valid SummaryContent content) {
}
