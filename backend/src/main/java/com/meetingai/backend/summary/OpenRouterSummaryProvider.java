package com.meetingai.backend.summary;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import jakarta.validation.Validator;
import tools.jackson.databind.ObjectMapper;

/**
 * Provider padrão da aplicação: plano free do OpenRouter, com a chave da
 * própria aplicação (não a do usuário). É o que roda quando o usuário não
 * configurou uma chave própria em Configurações.
 */
@Service
public class OpenRouterSummaryProvider extends AbstractLlmSummaryProvider implements SummaryProvider {

	private final OpenAiCompatibleChatClient client;
	private final String apiKey;
	private final String model;

	public OpenRouterSummaryProvider(RestClient.Builder restClientBuilder, ObjectMapper objectMapper, Validator validator,
			@Value("${app.summary.openrouter.base-url}") String baseUrl,
			@Value("${app.summary.openrouter.api-key}") String apiKey,
			@Value("${app.summary.openrouter.model}") String model) {
		super(objectMapper, validator);
		this.client = new OpenAiCompatibleChatClient(restClientBuilder, baseUrl);
		this.apiKey = apiKey;
		this.model = model;
	}

	@Override
	public SummaryContent summarize(String transcriptionText) {
		String raw = client.complete(apiKey, model, buildPrompt(transcriptionText));
		return parseAndValidate(raw);
	}

}
