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
		Instant sentToCrmAt) {

	public static MeetingResponse from(Meeting meeting) {
		return new MeetingResponse(
				meeting.getId(),
				meeting.getTitle(),
				meeting.getOriginalFilename(),
				meeting.getStatus(),
				meeting.getUploadedAt(),
				meeting.getExpiresAt(),
				meeting.getSentToCrmAt());
	}

}
