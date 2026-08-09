package com.meetingai.backend.transcription;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.meetingai.backend.security.CurrentUserService;
import com.meetingai.backend.user.User;

@RestController
public class TranscriptionController {

	private final CurrentUserService currentUserService;
	private final TranscriptionService transcriptionService;

	public TranscriptionController(CurrentUserService currentUserService, TranscriptionService transcriptionService) {
		this.currentUserService = currentUserService;
		this.transcriptionService = transcriptionService;
	}

	@GetMapping("/api/meetings/{meetingId}/transcription")
	public ResponseEntity<TranscriptionResponse> get(@PathVariable UUID meetingId) {
		User user = currentUserService.getCurrentUser();
		return ResponseEntity.ok(transcriptionService.getForMeeting(user, meetingId));
	}

}
