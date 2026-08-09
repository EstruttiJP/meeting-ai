package com.meetingai.backend.user;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

	@GetMapping("/me")
	public ResponseEntity<UserResponse> me() {
		// TODO: depende do login OAuth2 (Google) para resolver o usuário autenticado.
		return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
	}

}
