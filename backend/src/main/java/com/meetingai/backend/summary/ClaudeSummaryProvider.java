package com.meetingai.backend.summary;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.meetingai.backend.aiprovider.AiProvider;

import jakarta.validation.Validator;
import tools.jackson.databind.ObjectMapper;

/** Chave própria do usuário para a API da Anthropic (Claude). */
@Service
public class ClaudeSummaryProvider extends AbstractLlmSummaryProvider implements UserKeySummaryProvider {

	private static final String ANTHROPIC_VERSION = "2023-06-01";
	private static final int MAX_TOKENS = 2048;

	private final RestClient restClient;
	private final String model;

	public ClaudeSummaryProvider(RestClient.Builder restClientBuilder, ObjectMapper objectMapper, Validator validator,
			@Value("${app.summary.claude.base-url}") String baseUrl,
			@Value("${app.summary.claude.model}") String model) {
		super(objectMapper, validator);
		this.restClient = restClientBuilder.baseUrl(baseUrl).build();
		this.model = model;
	}

	@Override
	public AiProvider supportedProvider() {
		return AiProvider.CLAUDE;
	}

	@Override
	public SummaryContent summarize(String transcriptionText, String apiKey) {
		String prompt = buildPrompt(transcriptionText);
		ClaudeResponse response;
		try {
			response = restClient.post()
					.uri("/messages")
					.header("x-api-key", apiKey)
					.header("anthropic-version", ANTHROPIC_VERSION)
					.contentType(MediaType.APPLICATION_JSON)
					.body(new ClaudeRequest(model, MAX_TOKENS, List.of(new ClaudeMessage("user", prompt))))
					.retrieve()
					.body(ClaudeResponse.class);
		} catch (RestClientException e) {
			throw new SummaryGenerationException("Falha ao chamar o Claude", e);
		}
		return parseAndValidate(extractText(response));
	}

	private String extractText(ClaudeResponse response) {
		if (response == null || response.content() == null || response.content().isEmpty()) {
			throw new SummaryGenerationException("Resposta vazia do Claude");
		}
		return response.content().get(0).text();
	}

	private record ClaudeMessage(String role, String content) {
	}

	private record ClaudeRequest(String model, @JsonProperty("max_tokens") int maxTokens, List<ClaudeMessage> messages) {
	}

	private record ClaudeBlock(String type, String text) {
	}

	private record ClaudeResponse(List<ClaudeBlock> content) {
	}

}
