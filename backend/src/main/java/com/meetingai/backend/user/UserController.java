package com.meetingai.backend.user;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meetingai.backend.security.CurrentUserService;

@RestController
@RequestMapping("/api/users")
public class UserController {

	private final CurrentUserService currentUserService;

	public UserController(CurrentUserService currentUserService) {
		this.currentUserService = currentUserService;
	}

	@GetMapping("/me")
	public ResponseEntity<UserResponse> me() {
		return ResponseEntity.ok(UserResponse.from(currentUserService.getCurrentUser()));
	}

}
