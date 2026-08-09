package com.meetingai.backend.summary;

import java.time.Instant;
import java.util.UUID;

import com.meetingai.backend.meeting.Meeting;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "summary")
public class Summary {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "meeting_id", nullable = false, unique = true)
	private Meeting meeting;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false)
	private String content;

	@Column(nullable = false)
	private boolean approved;

	@Column(name = "approved_at")
	private Instant approvedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Summary() {
	}

	public Summary(Meeting meeting, String content) {
		this.meeting = meeting;
		this.content = content;
		this.approved = false;
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

	public UUID getId() {
		return id;
	}

	public Meeting getMeeting() {
		return meeting;
	}

	public String getContent() {
		return content;
	}

	public boolean isApproved() {
		return approved;
	}

	public Instant getApprovedAt() {
		return approvedAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

}
