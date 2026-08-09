package com.meetingai.backend.aiprovider;

import java.util.List;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AiProviderConfigServiceTest {

	@Mock
	private AiProviderConfigRepository repository;

	@Mock
	private EncryptionService encryptionService;

	private AiProviderConfigService service;
	private User user;

	@BeforeEach
	void setUp() {
		service = new AiProviderConfigService(repository, encryptionService);
		user = new User("google-sub-1", "dev@meetingai.com", "Dev User", null);
		ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
	}

	@Test
	void encryptsApiKeyBeforePersisting() {
		given(repository.findByUserId(user.getId())).willReturn(List.of());
		given(repository.findByUserIdAndProvider(user.getId(), AiProvider.GEMINI)).willReturn(Optional.empty());
		given(repository.save(any(AiProviderConfig.class))).willAnswer(invocation -> invocation.getArgument(0));
		given(encryptionService.encrypt("sk-secret")).willReturn("encrypted-sk-secret");
		AiProviderConfigRequest request = new AiProviderConfigRequest(AiProvider.GEMINI, "sk-secret", true);

		AiProviderConfig saved = service.save(user, request);

		assertThat(saved.getApiKeyEncrypted()).isEqualTo("encrypted-sk-secret");
	}

	@Test
	void blankApiKeyIsStoredAsNull() {
		given(repository.findByUserId(user.getId())).willReturn(List.of());
		given(repository.findByUserIdAndProvider(user.getId(), AiProvider.OPENROUTER)).willReturn(Optional.empty());
		given(repository.save(any(AiProviderConfig.class))).willAnswer(invocation -> invocation.getArgument(0));
		AiProviderConfigRequest request = new AiProviderConfigRequest(AiProvider.OPENROUTER, "  ", true);

		AiProviderConfig saved = service.save(user, request);

		assertThat(saved.getApiKeyEncrypted()).isNull();
	}

	@Test
	void savingReplacesExistingConfigForSameProvider() {
		AiProviderConfig existing = new AiProviderConfig(user, AiProvider.GEMINI, "old-encrypted", false);
		given(repository.findByUserIdAndProvider(user.getId(), AiProvider.GEMINI)).willReturn(Optional.of(existing));
		given(repository.save(any(AiProviderConfig.class))).willAnswer(invocation -> invocation.getArgument(0));
		given(encryptionService.encrypt("sk-new")).willReturn("new-encrypted");
		AiProviderConfigRequest request = new AiProviderConfigRequest(AiProvider.GEMINI, "sk-new", false);

		service.save(user, request);

		verify(repository).delete(existing);
	}

	@Test
	void markingNewConfigAsDefaultClearsOtherDefaults() {
		AiProviderConfig otherDefault = new AiProviderConfig(user, AiProvider.OPENAI, "openai-encrypted", true);
		given(repository.findByUserId(user.getId())).willReturn(List.of(otherDefault));
		given(repository.findByUserIdAndProvider(user.getId(), AiProvider.GEMINI)).willReturn(Optional.empty());
		given(repository.save(any(AiProviderConfig.class))).willAnswer(invocation -> invocation.getArgument(0));
		given(encryptionService.encrypt("sk-new")).willReturn("new-encrypted");
		AiProviderConfigRequest request = new AiProviderConfigRequest(AiProvider.GEMINI, "sk-new", true);

		service.save(user, request);

		assertThat(otherDefault.isDefault()).isFalse();
		verify(repository).save(otherDefault);
	}

	@Test
	void deleteRemovesConfigOwnedByUser() {
		UUID configId = UUID.randomUUID();
		AiProviderConfig config = new AiProviderConfig(user, AiProvider.GEMINI, "encrypted", false);
		given(repository.findById(configId)).willReturn(Optional.of(config));

		service.delete(user, configId);

		verify(repository).delete(config);
	}

	@Test
	void deleteThrowsWhenConfigBelongsToAnotherUser() {
		UUID configId = UUID.randomUUID();
		User otherUser = new User("google-sub-2", "other@meetingai.com", "Other User", null);
		ReflectionTestUtils.setField(otherUser, "id", UUID.randomUUID());
		AiProviderConfig config = new AiProviderConfig(otherUser, AiProvider.GEMINI, "encrypted", false);
		given(repository.findById(configId)).willReturn(Optional.of(config));

		assertThatThrownBy(() -> service.delete(user, configId))
				.isInstanceOf(AiProviderConfigNotFoundException.class);
		verify(repository, never()).delete(any(AiProviderConfig.class));
	}

	@Test
	void deleteThrowsWhenConfigDoesNotExist() {
		UUID configId = UUID.randomUUID();
		given(repository.findById(configId)).willReturn(Optional.empty());

		assertThatThrownBy(() -> service.delete(user, configId))
				.isInstanceOf(AiProviderConfigNotFoundException.class);
	}

}
