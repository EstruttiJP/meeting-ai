package com.meetingai.backend.crm;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.meetingai.backend.crypto.EncryptionService;
import com.meetingai.backend.user.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CrmConnectionServiceTest {

	@Mock
	private CrmConnectionRepository repository;

	@Mock
	private PipedriveOAuthClient pipedriveOAuthClient;

	@Mock
	private EncryptionService encryptionService;

	private CrmConnectionService service;
	private User user;

	@BeforeEach
	void setUp() {
		service = new CrmConnectionService(repository, pipedriveOAuthClient, encryptionService);
		user = new User("google-sub-1", "dev@meetingai.com", "Dev User", null);
		ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
	}

	@Test
	void connectExchangesCodeAndStoresEncryptedTokens() {
		given(repository.findByUserId(user.getId())).willReturn(Optional.empty());
		given(repository.save(any(CrmConnection.class))).willAnswer(invocation -> invocation.getArgument(0));
		PipedriveTokens tokens = new PipedriveTokens("access-123", "refresh-456", Instant.now().plusSeconds(3600));
		given(pipedriveOAuthClient.exchangeAuthorizationCode("auth-code")).willReturn(tokens);
		given(encryptionService.encrypt("access-123")).willReturn("encrypted-access");
		given(encryptionService.encrypt("refresh-456")).willReturn("encrypted-refresh");
		CrmConnectionRequest request = new CrmConnectionRequest(CrmProvider.PIPEDRIVE, "auth-code");

		CrmConnection connection = service.connect(user, request);

		assertThat(connection.getAccessTokenEncrypted()).isEqualTo("encrypted-access");
		assertThat(connection.getRefreshTokenEncrypted()).isEqualTo("encrypted-refresh");
		assertThat(connection.getProvider()).isEqualTo(CrmProvider.PIPEDRIVE);
	}

	@Test
	void connectReplacesExistingConnectionForUser() {
		CrmConnection existing = new CrmConnection(user, CrmProvider.PIPEDRIVE, "old-access", null, null);
		given(repository.findByUserId(user.getId())).willReturn(Optional.of(existing));
		given(repository.save(any(CrmConnection.class))).willAnswer(invocation -> invocation.getArgument(0));
		PipedriveTokens tokens = new PipedriveTokens("access-new", null, null);
		given(pipedriveOAuthClient.exchangeAuthorizationCode("auth-code")).willReturn(tokens);
		given(encryptionService.encrypt("access-new")).willReturn("encrypted-access-new");
		CrmConnectionRequest request = new CrmConnectionRequest(CrmProvider.PIPEDRIVE, "auth-code");

		service.connect(user, request);

		verify(repository).delete(existing);
	}

	@Test
	void disconnectRemovesConnectionWhenPresent() {
		CrmConnection existing = new CrmConnection(user, CrmProvider.PIPEDRIVE, "access", null, null);
		given(repository.findByUserId(user.getId())).willReturn(Optional.of(existing));

		service.disconnect(user);

		verify(repository).delete(existing);
	}

}
