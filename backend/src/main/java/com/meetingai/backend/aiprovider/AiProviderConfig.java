package com.meetingai.backend.aiprovider;

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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "ai_provider_config")
public class AiProviderConfig {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private AiProvider provider;

	@Column(name = "api_key_encrypted")
	private String apiKeyEncrypted;

	@Column(name = "is_default", nullable = false)
	private boolean isDefault;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected AiProviderConfig() {
	}

	public AiProviderConfig(User user, AiProvider provider, String apiKeyEncrypted, boolean isDefault) {
		this.user = user;
		this.provider = provider;
		this.apiKeyEncrypted = apiKeyEncrypted;
		this.isDefault = isDefault;
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

	public User getUser() {
		return user;
	}

	public AiProvider getProvider() {
		return provider;
	}

	public String getApiKeyEncrypted() {
		return apiKeyEncrypted;
	}

	public boolean isDefault() {
		return isDefault;
	}

	public void clearDefault() {
		this.isDefault = false;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

}
