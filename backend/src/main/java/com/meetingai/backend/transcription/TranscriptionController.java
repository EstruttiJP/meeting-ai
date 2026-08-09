package com.meetingai.backend.transcription;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TranscriptionController {

	@GetMapping("/api/meetings/{meetingId}/transcription")
	public ResponseEntity<TranscriptionResponse> get(@PathVariable UUID meetingId) {
		// TODO: depende do login OAuth2 (Google) para resolver o usuário autenticado.
		return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
	}

}
