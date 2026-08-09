package com.meetingai.backend.summary;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Chamada HTTP para a API de chat completions no formato OpenAI — usada tanto
 * pelo OpenRouter quanto pela OpenAI direto, já que o OpenRouter replica esse
 * mesmo formato de request/response.
 */
final class OpenAiCompatibleChatClient {

	private final RestClient restClient;

	OpenAiCompatibleChatClient(RestClient.Builder restClientBuilder, String baseUrl) {
		this.restClient = restClientBuilder.baseUrl(baseUrl).build();
	}

	String complete(String apiKey, String model, String prompt) {
		ChatCompletionResponse response;
		try {
			response = restClient.post()
					.uri("/chat/completions")
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
					.contentType(MediaType.APPLICATION_JSON)
					.body(new ChatCompletionRequest(model, List.of(new ChatMessage("user", prompt))))
					.retrieve()
					.body(ChatCompletionResponse.class);
		} catch (RestClientException e) {
			throw new SummaryGenerationException("Falha ao chamar a API de IA", e);
		}
		if (response == null || response.choices() == null || response.choices().isEmpty()) {
			throw new SummaryGenerationException("Resposta vazia da API de IA");
		}
		return response.choices().get(0).message().content();
	}

	private record ChatMessage(String role, String content) {
	}

	private record ChatCompletionRequest(String model, List<ChatMessage> messages) {
	}

	private record Choice(ChatMessage message) {
	}

	private record ChatCompletionResponse(List<Choice> choices) {
	}

}
