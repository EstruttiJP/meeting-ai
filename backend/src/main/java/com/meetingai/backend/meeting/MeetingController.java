package com.meetingai.backend.meeting;

import java.util.List;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meetingai.backend.meeting.MeetingAudioService.MeetingAudio;
import com.meetingai.backend.security.CurrentUserService;
import com.meetingai.backend.user.User;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/meetings")
public class MeetingController {

	private final CurrentUserService currentUserService;
	private final MeetingUploadService meetingUploadService;
	private final MeetingPipelineService meetingPipelineService;
	private final MeetingAccessService meetingAccessService;
	private final MeetingAudioService meetingAudioService;

	public MeetingController(CurrentUserService currentUserService, MeetingUploadService meetingUploadService,
			MeetingPipelineService meetingPipelineService, MeetingAccessService meetingAccessService,
			MeetingAudioService meetingAudioService) {
		this.currentUserService = currentUserService;
		this.meetingUploadService = meetingUploadService;
		this.meetingPipelineService = meetingPipelineService;
		this.meetingAccessService = meetingAccessService;
		this.meetingAudioService = meetingAudioService;
	}

	@GetMapping
	public ResponseEntity<List<MeetingResponse>> list() {
		User user = currentUserService.getCurrentUser();
		List<MeetingResponse> responses = meetingAccessService.listForUser(user).stream()
				.map(MeetingResponse::from)
				.toList();
		return ResponseEntity.ok(responses);
	}

	@PostMapping(consumes = "multipart/form-data")
	public ResponseEntity<MeetingResponse> upload(@Valid @ModelAttribute MeetingUploadRequest request) {
		User user = currentUserService.getCurrentUser();
		Meeting meeting = meetingUploadService.upload(user, request.getTitle(), request.getFile());
		// Dispara o pipeline em segundo plano — a resposta não espera transcrição/resumo terminar.
		meetingPipelineService.process(meeting.getId());
		return ResponseEntity.status(HttpStatus.CREATED).body(MeetingResponse.from(meeting));
	}

	@GetMapping("/{id}")
	public ResponseEntity<MeetingResponse> get(@PathVariable UUID id) {
		User user = currentUserService.getCurrentUser();
		Meeting meeting = meetingAccessService.getOwnedMeeting(user, id);
		return ResponseEntity.ok(MeetingResponse.from(meeting));
	}

	/**
	 * Áudio original para o player da tela de revisão. Responde 410 quando o
	 * arquivo já foi apagado pela retenção, para a tela distinguir "expirou" de
	 * "deu erro".
	 */
	@GetMapping("/{id}/audio")
	public ResponseEntity<Resource> audio(@PathVariable UUID id) {
		User user = currentUserService.getCurrentUser();
		MeetingAudio audio = meetingAudioService.load(user, id);
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(audio.contentType()))
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.inline().filename(audio.filename()).toString())
				.body(audio.resource());
	}

}
