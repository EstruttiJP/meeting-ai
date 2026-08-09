package com.meetingai.backend.aiprovider;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetingai.backend.crypto.EncryptionService;
import com.meetingai.backend.user.User;

/**
 * Só um provider pode ser o padrão do usuário por vez — quem chama
 * {@link com.meetingai.backend.summary.SummaryOrchestrator} depende disso
 * pra saber qual chave usar. A chave nunca é logada nem devolvida em claro
 * (ver {@link AiProviderConfigResponse}, que só expõe um boolean).
 */
@Service
public class AiProviderConfigService {

	private final AiProviderConfigRepository repository;
	private final EncryptionService encryptionService;

	public AiProviderConfigService(AiProviderConfigRepository repository, EncryptionService encryptionService) {
		this.repository = repository;
		this.encryptionService = encryptionService;
	}

	public List<AiProviderConfig> list(User user) {
		return repository.findByUserId(user.getId());
	}

	@Transactional
	public AiProviderConfig save(User user, AiProviderConfigRequest request) {
		if (request.isDefault()) {
			clearOtherDefaults(user, request.provider());
		}
		repository.findByUserIdAndProvider(user.getId(), request.provider()).ifPresent(repository::delete);

		String encryptedKey = (request.apiKey() == null || request.apiKey().isBlank())
				? null
				: encryptionService.encrypt(request.apiKey());
		AiProviderConfig config = new AiProviderConfig(user, request.provider(), encryptedKey, request.isDefault());
		return repository.save(config);
	}

	@Transactional
	public void delete(User user, UUID configId) {
		AiProviderConfig config = repository.findById(configId)
				.filter(c -> c.getUser().getId().equals(user.getId()))
				.orElseThrow(() -> new AiProviderConfigNotFoundException(configId));
		repository.delete(config);
	}

	private void clearOtherDefaults(User user, AiProvider provider) {
		repository.findByUserId(user.getId()).stream()
				.filter(AiProviderConfig::isDefault)
				.filter(existing -> existing.getProvider() != provider)
				.forEach(existing -> {
					existing.clearDefault();
					repository.save(existing);
				});
	}

}
