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

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/meetings/{meetingId}/summary")
public class SummaryController {

	@GetMapping
	public ResponseEntity<SummaryResponse> get(@PathVariable UUID meetingId) {
		// TODO: depende do login OAuth2 (Google) para resolver o usuário autenticado.
		return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
	}

	@PutMapping
	public ResponseEntity<SummaryResponse> update(@PathVariable UUID meetingId,
			@Valid @RequestBody SummaryUpdateRequest request) {
		// TODO: depende do login OAuth2 (Google) para resolver o usuário autenticado.
		return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
	}

	@PostMapping("/send-to-crm")
	public ResponseEntity<Void> sendToCrm(@PathVariable UUID meetingId) {
		// TODO: requer integração Pipedrive.
		return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
	}

}
