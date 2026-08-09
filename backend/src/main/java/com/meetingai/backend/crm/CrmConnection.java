package com.meetingai.backend.crm;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "crm_connection")
public class CrmConnection {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false, unique = true)
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private CrmProvider provider;

	@Column(name = "access_token_encrypted", nullable = false)
	private String accessTokenEncrypted;

	@Column(name = "refresh_token_encrypted")
	private String refreshTokenEncrypted;

	@Column(name = "token_expires_at")
	private Instant tokenExpiresAt;

	@Column(name = "connected_at", nullable = false, updatable = false)
	private Instant connectedAt;

	protected CrmConnection() {
	}

	public CrmConnection(User user, CrmProvider provider, String accessTokenEncrypted,
			String refreshTokenEncrypted, Instant tokenExpiresAt) {
		this.user = user;
		this.provider = provider;
		this.accessTokenEncrypted = accessTokenEncrypted;
		this.refreshTokenEncrypted = refreshTokenEncrypted;
		this.tokenExpiresAt = tokenExpiresAt;
	}

	@PrePersist
	void onCreate() {
		if (connectedAt == null) {
			connectedAt = Instant.now();
		}
	}

	public UUID getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public CrmProvider getProvider() {
		return provider;
	}

	public String getAccessTokenEncrypted() {
		return accessTokenEncrypted;
	}

	public String getRefreshTokenEncrypted() {
		return refreshTokenEncrypted;
	}

	public Instant getTokenExpiresAt() {
		return tokenExpiresAt;
	}

	public Instant getConnectedAt() {
		return connectedAt;
	}

}
