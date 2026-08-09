package com.meetingai.backend.meeting;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meetingai.backend.security.CurrentUserService;
import com.meetingai.backend.user.User;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/meetings")
public class MeetingController {

	private final CurrentUserService currentUserService;
	private final MeetingUploadService meetingUploadService;

	public MeetingController(CurrentUserService currentUserService, MeetingUploadService meetingUploadService) {
		this.currentUserService = currentUserService;
		this.meetingUploadService = meetingUploadService;
	}

	@GetMapping
	public ResponseEntity<List<MeetingResponse>> list() {
		// TODO: item de escopo separado (listagem + paginação do dashboard).
		return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
	}

	@PostMapping(consumes = "multipart/form-data")
	public ResponseEntity<MeetingResponse> upload(@Valid @ModelAttribute MeetingUploadRequest request) {
		User user = currentUserService.getCurrentUser();
		Meeting meeting = meetingUploadService.upload(user, request.getTitle(), request.getFile());
		return ResponseEntity.status(HttpStatus.CREATED).body(MeetingResponse.from(meeting));
	}

	@GetMapping("/{id}")
	public ResponseEntity<MeetingResponse> get(@PathVariable UUID id) {
		// TODO: item de escopo separado (detalhe/status da reunião).
		return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
	}

}
