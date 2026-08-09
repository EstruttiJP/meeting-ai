package com.meetingai.backend.aiprovider;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/ai-provider-configs")
public class AiProviderConfigController {

	@GetMapping
	public ResponseEntity<List<AiProviderConfigResponse>> list() {
		// TODO: depende do login OAuth2 (Google) para resolver o usuário autenticado.
		return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
	}

	@PostMapping
	public ResponseEntity<AiProviderConfigResponse> create(@Valid @RequestBody AiProviderConfigRequest request) {
		// TODO: requer criptografia da chave antes de persistir.
		return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
	}

}
