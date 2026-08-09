package com.meetingai.backend.crm;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetingai.backend.crypto.EncryptionService;
import com.meetingai.backend.user.User;

@Service
public class CrmConnectionService {

	private final CrmConnectionRepository repository;
	private final PipedriveOAuthClient pipedriveOAuthClient;
	private final EncryptionService encryptionService;

	public CrmConnectionService(CrmConnectionRepository repository, PipedriveOAuthClient pipedriveOAuthClient,
			EncryptionService encryptionService) {
		this.repository = repository;
		this.pipedriveOAuthClient = pipedriveOAuthClient;
		this.encryptionService = encryptionService;
	}

	public Optional<CrmConnection> findForUser(User user) {
		return repository.findByUserId(user.getId());
	}

	@Transactional
	public CrmConnection connect(User user, CrmConnectionRequest request) {
		PipedriveTokens tokens = pipedriveOAuthClient.exchangeAuthorizationCode(request.authorizationCode());
		repository.findByUserId(user.getId()).ifPresent(repository::delete);

		String encryptedAccessToken = encryptionService.encrypt(tokens.accessToken());
		String encryptedRefreshToken = tokens.refreshToken() == null ? null : encryptionService.encrypt(tokens.refreshToken());
		CrmConnection connection = new CrmConnection(
				user, request.provider(), encryptedAccessToken, encryptedRefreshToken, tokens.expiresAt());
		return repository.save(connection);
	}

	@Transactional
	public void disconnect(User user) {
		repository.findByUserId(user.getId()).ifPresent(repository::delete);
	}

}
