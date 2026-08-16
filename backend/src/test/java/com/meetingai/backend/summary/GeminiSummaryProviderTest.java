package com.meetingai.backend.summary;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.meetingai.backend.aiprovider.AiProvider;
import com.meetingai.backend.transcription.TranscriptionResult;
import com.meetingai.backend.transcription.TranscriptionSegment;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeminiSummaryProviderTest {

	private final ObjectMapper objectMapper = JsonMapper.builder().build();
	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	private MockRestServiceServer mockServer;
	private GeminiSummaryProvider provider;

	private static final TranscriptionResult TRANSCRICAO = new TranscriptionResult(
			"texto completo", "pt",
			List.of(new TranscriptionSegment(0, 8, "Abertura"),
					new TranscriptionSegment(30, 38, "Meio da conversa")));

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		mockServer = MockRestServiceServer.bindTo(builder).build();
		provider = new GeminiSummaryProvider(builder, objectMapper, validator,
				"https://gemini.local/v1beta", "gemini-test-model");
	}

	@Test
	void reportsGeminiAsSupportedProvider() {
		assertThat(provider.supportedProvider()).isEqualTo(AiProvider.GEMINI);
	}

	@Test
	void summarizesValidJsonResponse() {
		String innerJson = """
				{"summary": "Reunião de descoberta", "items": [
				  {"type": "proximo_passo", "content": "Enviar proposta", "timestampSeconds": 30},
				  {"type": "ponto_atencao", "content": "Preço alto", "timestampSeconds": 0}]}
				""";
		mockServer.expect(requestTo(containsString(":generateContent")))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header("x-goog-api-key", "user-api-key"))
				.andRespond(withSuccess(geminiResponse(innerJson), MediaType.APPLICATION_JSON));

		SummaryContent content = provider.summarize(TRANSCRICAO, "user-api-key");

		assertThat(content.summary()).isEqualTo("Reunião de descoberta");
		assertThat(content.itemsOfType(SummaryItemType.PROXIMO_PASSO))
				.singleElement().satisfies(item -> assertThat(item.content()).isEqualTo("Enviar proposta"));
		assertThat(content.itemsOfType(SummaryItemType.PONTO_ATENCAO))
				.singleElement().satisfies(item -> assertThat(item.content()).isEqualTo("Preço alto"));
	}

	@Test
	void apiKeyNeverAppearsInRequestUriOrFailureMessage() {
		mockServer.expect(requestTo(containsString(":generateContent")))
				.andRespond(withServerError());

		assertThatThrownBy(() -> provider.summarize(TRANSCRICAO, "user-api-key"))
				.isInstanceOf(SummaryGenerationException.class)
				.satisfies(ex -> assertThat(rootMessageChain(ex)).doesNotContain("user-api-key"));
	}

	private String rootMessageChain(Throwable throwable) {
		StringBuilder chain = new StringBuilder();
		for (Throwable current = throwable; current != null; current = current.getCause()) {
			chain.append(current.getMessage()).append(" | ");
		}
		return chain.toString();
	}

	private String geminiResponse(String text) {
		Map<String, Object> part = Map.of("text", text);
		Map<String, Object> content = Map.of("parts", List.of(part));
		Map<String, Object> candidate = Map.of("content", content);
		Map<String, Object> body = Map.of("candidates", List.of(candidate));
		return objectMapper.writeValueAsString(body);
	}

}
