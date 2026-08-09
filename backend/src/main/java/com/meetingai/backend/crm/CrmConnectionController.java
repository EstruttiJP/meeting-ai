package com.meetingai.backend.crm;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meetingai.backend.security.CurrentUserService;
import com.meetingai.backend.user.User;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/crm-connections")
public class CrmConnectionController {

	private final CurrentUserService currentUserService;
	private final CrmConnectionService crmConnectionService;

	public CrmConnectionController(CurrentUserService currentUserService, CrmConnectionService crmConnectionService) {
		this.currentUserService = currentUserService;
		this.crmConnectionService = crmConnectionService;
	}

	@GetMapping("/me")
	public ResponseEntity<CrmConnectionResponse> me() {
		User user = currentUserService.getCurrentUser();
		return crmConnectionService.findForUser(user)
				.map(CrmConnectionResponse::from)
				.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@PostMapping
	public ResponseEntity<CrmConnectionResponse> connect(@Valid @RequestBody CrmConnectionRequest request) {
		User user = currentUserService.getCurrentUser();
		CrmConnection connection = crmConnectionService.connect(user, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(CrmConnectionResponse.from(connection));
	}

	@DeleteMapping("/me")
	public ResponseEntity<Void> disconnect() {
		User user = currentUserService.getCurrentUser();
		crmConnectionService.disconnect(user);
		return ResponseEntity.noContent().build();
	}

}
