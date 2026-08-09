package com.meetingai.backend.usagequota;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usage-quota")
public class UsageQuotaController {

	@GetMapping("/me")
	public ResponseEntity<UsageQuotaResponse> me() {
		// TODO: depende do login OAuth2 (Google) para resolver o usuário autenticado.
		return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
	}

}
