package com.meetingai.backend.summary;

import java.time.Instant;
import java.util.UUID;

public record SummaryResponse(
		UUID id,
		UUID meetingId,
		SummaryContent content,
		boolean approved,
		Instant approvedAt) {
}
