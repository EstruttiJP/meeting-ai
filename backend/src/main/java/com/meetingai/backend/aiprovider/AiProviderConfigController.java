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

import com.meetingai.backend.security.CurrentUserService;
import com.meetingai.backend.user.User;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/ai-provider-configs")
public class AiProviderConfigController {

	private final CurrentUserService currentUserService;
	private final AiProviderConfigService aiProviderConfigService;

	public AiProviderConfigController(CurrentUserService currentUserService, AiProviderConfigService aiProviderConfigService) {
		this.currentUserService = currentUserService;
		this.aiProviderConfigService = aiProviderConfigService;
	}

	@GetMapping
	public ResponseEntity<List<AiProviderConfigResponse>> list() {
		User user = currentUserService.getCurrentUser();
		List<AiProviderConfigResponse> responses = aiProviderConfigService.list(user).stream()
				.map(AiProviderConfigResponse::from)
				.toList();
		return ResponseEntity.ok(responses);
	}

	@PostMapping
	public ResponseEntity<AiProviderConfigResponse> create(@Valid @RequestBody AiProviderConfigRequest request) {
		User user = currentUserService.getCurrentUser();
		AiProviderConfig config = aiProviderConfigService.save(user, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(AiProviderConfigResponse.from(config));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		User user = currentUserService.getCurrentUser();
		aiProviderConfigService.delete(user, id);
		return ResponseEntity.noContent().build();
	}

}
