package com.meetingai.backend.summary;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.meetingai.backend.aiprovider.AiProvider;
import com.meetingai.backend.aiprovider.AiProviderConfig;
import com.meetingai.backend.aiprovider.AiProviderConfigRepository;
import com.meetingai.backend.crypto.EncryptionService;
import com.meetingai.backend.user.User;

/**
 * Decide qual {@link SummaryProvider}/{@link UserKeySummaryProvider} chamar:
 * a configuração de IA padrão do usuário, se ele colou uma chave própria, ou
 * o OpenRouter (plano free da aplicação) como fallback.
 */
@Service
public class SummaryOrchestrator {

	private final SummaryProvider defaultProvider;
	private final Map<AiProvider, UserKeySummaryProvider> userKeyProviders;
	private final AiProviderConfigRepository aiProviderConfigRepository;
	private final EncryptionService encryptionService;

	public SummaryOrchestrator(
			OpenRouterSummaryProvider defaultProvider,
			List<UserKeySummaryProvider> userKeyProviders,
			AiProviderConfigRepository aiProviderConfigRepository,
			EncryptionService encryptionService) {
		this.defaultProvider = defaultProvider;
		this.userKeyProviders = userKeyProviders.stream()
				.collect(Collectors.toMap(UserKeySummaryProvider::supportedProvider, Function.identity()));
		this.aiProviderConfigRepository = aiProviderConfigRepository;
		this.encryptionService = encryptionService;
	}

	public SummaryContent summarize(User user, String transcriptionText) {
		return aiProviderConfigRepository.findByUserId(user.getId()).stream()
				.filter(AiProviderConfig::isDefault)
				.findFirst()
				.filter(config -> config.getProvider() != AiProvider.OPENROUTER)
				.map(config -> summarizeWithUserKey(config, transcriptionText))
				.orElseGet(() -> defaultProvider.summarize(transcriptionText));
	}

	private SummaryContent summarizeWithUserKey(AiProviderConfig config, String transcriptionText) {
		UserKeySummaryProvider provider = userKeyProviders.get(config.getProvider());
		if (provider == null) {
			throw new SummaryGenerationException("Provider de IA não suportado: " + config.getProvider());
		}
		String apiKey = encryptionService.decrypt(config.getApiKeyEncrypted());
		return provider.summarize(transcriptionText, apiKey);
	}

}
