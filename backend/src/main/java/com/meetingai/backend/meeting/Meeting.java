package com.meetingai.backend.meeting;

import java.time.Instant;
import java.util.UUID;

import com.meetingai.backend.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "meeting")
public class Meeting {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false)
	private String title;

	@Column(name = "original_filename", nullable = false)
	private String originalFilename;

	@Column(name = "storage_key", nullable = false)
	private String storageKey;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private MeetingStatus status;

	@Enumerated(EnumType.STRING)
	@Column(name = "meeting_type", nullable = false, length = 30)
	private MeetingType meetingType;

	@Column(name = "uploaded_at", nullable = false, updatable = false)
	private Instant uploadedAt;

	@Column(name = "expires_at")
	private Instant expiresAt;

	@Column(name = "sent_to_crm_at")
	private Instant sentToCrmAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "failure_category", length = 30)
	private MeetingFailureCategory failureCategory;

	@Column(name = "failure_reason", columnDefinition = "text")
	private String failureReason;

	protected Meeting() {
	}

	public Meeting(User user, String title, String originalFilename, String storageKey, MeetingType meetingType) {
		this.user = user;
		this.title = title;
		this.originalFilename = originalFilename;
		this.storageKey = storageKey;
		this.meetingType = meetingType == null ? MeetingType.GENERICA : meetingType;
		this.status = MeetingStatus.UPLOADED;
	}

	@PrePersist
	void onCreate() {
		if (uploadedAt == null) {
			uploadedAt = Instant.now();
		}
	}

	public void markTranscribing() {
		this.status = MeetingStatus.TRANSCRIBING;
	}

	public void markSummarizing() {
		this.status = MeetingStatus.SUMMARIZING;
	}

	public void markReady() {
		this.status = MeetingStatus.READY;
	}

	/**
	 * O motivo técnico é obrigatório: uma reunião em FAILED sem causa registrada
	 * é justamente o buraco de diagnóstico que essas colunas existem para fechar.
	 */
	public void markFailed(MeetingFailureCategory category, String reason) {
		if (category == null || reason == null || reason.isBlank()) {
			throw new IllegalArgumentException("Falha de reunião exige categoria e motivo técnico");
		}
		this.status = MeetingStatus.FAILED;
		this.failureCategory = category;
		this.failureReason = reason;
	}

	public void markSentToCrm() {
		this.status = MeetingStatus.SENT_TO_CRM;
		this.sentToCrmAt = Instant.now();
	}

	public void markExpired() {
		this.status = MeetingStatus.EXPIRED;
	}

	public void scheduleExpiration(Instant expiresAt) {
		this.expiresAt = expiresAt;
	}

	public UUID getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public String getTitle() {
		return title;
	}

	public String getOriginalFilename() {
		return originalFilename;
	}

	public String getStorageKey() {
		return storageKey;
	}

	public MeetingStatus getStatus() {
		return status;
	}

	public MeetingType getMeetingType() {
		return meetingType;
	}

	public Instant getUploadedAt() {
		return uploadedAt;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public Instant getSentToCrmAt() {
		return sentToCrmAt;
	}

	public MeetingFailureCategory getFailureCategory() {
		return failureCategory;
	}

	public String getFailureReason() {
		return failureReason;
	}

}
