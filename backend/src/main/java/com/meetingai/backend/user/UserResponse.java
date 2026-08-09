package com.meetingai.backend.user;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
		UUID id,
		String email,
		String name,
		String pictureUrl,
		Instant createdAt) {

	public static UserResponse from(User user) {
		return new UserResponse(
				user.getId(),
				user.getEmail(),
				user.getName(),
				user.getPictureUrl(),
				user.getCreatedAt());
	}

}
