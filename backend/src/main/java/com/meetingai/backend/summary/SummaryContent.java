package com.meetingai.backend.summary;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

public record SummaryContent(
		@NotBlank String summary,
		List<String> decisions,
		List<String> nextSteps,
		List<String> mentionedValues,
		String paymentMethod,
		List<String> objections) {
}
