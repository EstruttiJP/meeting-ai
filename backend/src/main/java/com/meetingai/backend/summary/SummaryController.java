package com.meetingai.backend.summary;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meetingai.backend.crm.CrmSyncService;
import com.meetingai.backend.security.CurrentUserService;
import com.meetingai.backend.user.User;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/meetings/{meetingId}/summary")
public class SummaryController {

	private final CurrentUserService currentUserService;
	private final SummaryService summaryService;
	private final CrmSyncService crmSyncService;

	public SummaryController(CurrentUserService currentUserService, SummaryService summaryService,
			CrmSyncService crmSyncService) {
		this.currentUserService = currentUserService;
		this.summaryService = summaryService;
		this.crmSyncService = crmSyncService;
	}

	@GetMapping
	public ResponseEntity<SummaryResponse> get(@PathVariable UUID meetingId) {
		User user = currentUserService.getCurrentUser();
		return ResponseEntity.ok(summaryService.getForMeeting(user, meetingId));
	}

	@PutMapping
	public ResponseEntity<SummaryResponse> update(@PathVariable UUID meetingId,
			@Valid @RequestBody SummaryUpdateRequest request) {
		User user = currentUserService.getCurrentUser();
		return ResponseEntity.ok(summaryService.approve(user, meetingId, request.content()));
	}

	// Edição granular: cada item é um recurso próprio, então corrigir uma
	// decisão não reescreve o resto do resumo junto — duas abas abertas na mesma
	// reunião não desfazem o trabalho uma da outra item a item.
	@PatchMapping
	public ResponseEntity<SummaryResponse> updateText(@PathVariable UUID meetingId,
			@Valid @RequestBody SummaryTextUpdateRequest request) {
		User user = currentUserService.getCurrentUser();
		return ResponseEntity.ok(summaryService.updateSummaryText(user, meetingId, request.summary()));
	}

	@PostMapping("/items")
	public ResponseEntity<SummaryResponse> addItem(@PathVariable UUID meetingId,
			@Valid @RequestBody SummaryItemCreateRequest request) {
		User user = currentUserService.getCurrentUser();
		return ResponseEntity.status(HttpStatus.CREATED).body(summaryService.addItem(user, meetingId, request));
	}

	@PatchMapping("/items/{itemId}")
	public ResponseEntity<SummaryResponse> updateItem(@PathVariable UUID meetingId, @PathVariable String itemId,
			@RequestBody SummaryItemUpdateRequest request) {
		User user = currentUserService.getCurrentUser();
		return ResponseEntity.ok(summaryService.updateItem(user, meetingId, itemId, request));
	}

	@DeleteMapping("/items/{itemId}")
	public ResponseEntity<SummaryResponse> removeItem(@PathVariable UUID meetingId, @PathVariable String itemId) {
		User user = currentUserService.getCurrentUser();
		return ResponseEntity.ok(summaryService.removeItem(user, meetingId, itemId));
	}

	@PostMapping("/send-to-crm")
	public ResponseEntity<Void> sendToCrm(@PathVariable UUID meetingId) {
		User user = currentUserService.getCurrentUser();
		crmSyncService.sendApprovedSummaryToCrm(user, meetingId);
		return ResponseEntity.noContent().build();
	}

}
