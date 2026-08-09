package com.meetingai.backend.summary;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.meetingai.backend.aiprovider.AiProvider;

import jakarta.validation.Validator;
import tools.jackson.databind.ObjectMapper;

/** Chave própria do usuário para a API do Google Gemini. */
@Service
public class GeminiSummaryProvider extends AbstractLlmSummaryProvider implements UserKeySummaryProvider {

	private final RestClient restClient;
	private final String model;

	public GeminiSummaryProvider(RestClient.Builder restClientBuilder, ObjectMapper objectMapper, Validator validator,
			@Value("${app.summary.gemini.base-url}") String baseUrl,
			@Value("${app.summary.gemini.model}") String model) {
		super(objectMapper, validator);
		this.restClient = restClientBuilder.baseUrl(baseUrl).build();
		this.model = model;
	}

	@Override
	public AiProvider supportedProvider() {
		return AiProvider.GEMINI;
	}

	@Override
	public SummaryContent summarize(String transcriptionText, String apiKey) {
		String prompt = buildPrompt(transcriptionText);
		GeminiResponse response;
		try {
			response = restClient.post()
					.uri(uriBuilder -> uriBuilder.path("/models/{model}:generateContent")
							.queryParam("key", apiKey)
							.build(model))
					.contentType(MediaType.APPLICATION_JSON)
					.body(new GeminiRequest(List.of(new GeminiContent(List.of(new GeminiPart(prompt))))))
					.retrieve()
					.body(GeminiResponse.class);
		} catch (RestClientException e) {
			throw new SummaryGenerationException("Falha ao chamar o Gemini", e);
		}
		return parseAndValidate(extractText(response));
	}

	private String extractText(GeminiResponse response) {
		if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
			throw new SummaryGenerationException("Resposta vazia do Gemini");
		}
		GeminiContent content = response.candidates().get(0).content();
		if (content == null || content.parts() == null || content.parts().isEmpty()) {
			throw new SummaryGenerationException("Resposta vazia do Gemini");
		}
		return content.parts().get(0).text();
	}

	private record GeminiPart(String text) {
	}

	private record GeminiContent(List<GeminiPart> parts) {
	}

	private record GeminiRequest(List<GeminiContent> contents) {
	}

	private record GeminiCandidate(GeminiContent content) {
	}

	private record GeminiResponse(List<GeminiCandidate> candidates) {
	}

}
