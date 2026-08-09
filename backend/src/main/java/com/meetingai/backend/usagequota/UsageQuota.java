package com.meetingai.backend.usagequota;

import java.time.Instant;
import java.util.UUID;

import com.meetingai.backend.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "usage_quota")
public class UsageQuota {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "month_reference", nullable = false, length = 7)
	private String monthReference;

	@Column(name = "meetings_uploaded", nullable = false)
	private int meetingsUploaded;

	@Column(name = "meetings_limit", nullable = false)
	private int meetingsLimit;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected UsageQuota() {
	}

	public UsageQuota(User user, String monthReference, int meetingsLimit) {
		this.user = user;
		this.monthReference = monthReference;
		this.meetingsLimit = meetingsLimit;
		this.meetingsUploaded = 0;
	}

	@PrePersist
	void onCreate() {
		Instant now = Instant.now();
		if (createdAt == null) {
			createdAt = now;
		}
		updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = Instant.now();
	}

	public boolean hasCapacity() {
		return meetingsUploaded < meetingsLimit;
	}

	public void incrementUsage() {
		this.meetingsUploaded++;
	}

	public UUID getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public String getMonthReference() {
		return monthReference;
	}

	public int getMeetingsUploaded() {
		return meetingsUploaded;
	}

	public int getMeetingsLimit() {
		return meetingsLimit;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

}
