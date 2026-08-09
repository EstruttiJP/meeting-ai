package com.meetingai.backend.crm;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/crm-connections")
public class CrmConnectionController {

	@GetMapping("/me")
	public ResponseEntity<CrmConnectionResponse> me() {
		// TODO: depende do login OAuth2 (Google) para resolver o usuário autenticado.
		return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
	}

	@PostMapping
	public ResponseEntity<CrmConnectionResponse> connect(@Valid @RequestBody CrmConnectionRequest request) {
		// TODO: requer troca do código OAuth do Pipedrive por token de acesso.
		return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
	}

	@DeleteMapping("/me")
	public ResponseEntity<Void> disconnect() {
		return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
	}

}
