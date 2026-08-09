package com.meetingai.backend.summary;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meetingai.backend.security.CurrentUserService;
import com.meetingai.backend.user.User;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/meetings/{meetingId}/summary")
public class SummaryController {

	private final CurrentUserService currentUserService;
	private final SummaryService summaryService;

	public SummaryController(CurrentUserService currentUserService, SummaryService summaryService) {
		this.currentUserService = currentUserService;
		this.summaryService = summaryService;
	}

	@GetMapping
	public ResponseEntity<SummaryResponse> get(@PathVariable UUID meetingId) {
		User user = currentUserService.getCurrentUser();
		return ResponseEntity.ok(summaryService.getForMeeting(user, meetingId));
	}

	@PutMapping
	public ResponseEntity<SummaryResponse> update(@PathVariable UUID meetingId,
			@Valid @RequestBody SummaryUpdateRequest request) {
		User user = currentUserService.getCurrentUser();
		return ResponseEntity.ok(summaryService.approve(user, meetingId, request.content()));
	}

	@PostMapping("/send-to-crm")
	public ResponseEntity<Void> sendToCrm(@PathVariable UUID meetingId) {
		// TODO: requer integração Pipedrive.
		return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
	}

}
