package com.meetingai.backend.transcription;

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
import jakarta.persistence.Table;

@Entity
@Table(name = "transcription")
public class Transcription {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "meeting_id", nullable = false, unique = true)
	private Meeting meeting;

	@Column(nullable = false, columnDefinition = "text")
	private String content;

	@Column(length = 10)
	private String language;

	@Column(nullable = false, length = 50)
	private String provider;

	/**
	 * Segmentos com timestamp, serializados como JSON. Fica nulo nas
	 * transcrições geradas antes de este recurso existir.
	 */
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "jsonb")
	private String segments;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected Transcription() {
	}

	public Transcription(Meeting meeting, String content, String language, String provider, String segments) {
		this.meeting = meeting;
		this.content = content;
		this.language = language;
		this.provider = provider;
		this.segments = segments;
	}

	@PrePersist
	void onCreate() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
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

	public String getLanguage() {
		return language;
	}

	public String getProvider() {
		return provider;
	}

	public String getSegments() {
		return segments;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
