package com.meetingai.backend.summary;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenRouterSummaryProviderTest {

	private final ObjectMapper objectMapper = JsonMapper.builder().build();
	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	private MockRestServiceServer mockServer;
	private OpenRouterSummaryProvider provider;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		mockServer = MockRestServiceServer.bindTo(builder).build();
		provider = new OpenRouterSummaryProvider(builder, objectMapper, validator,
				"https://openrouter.local/api/v1", "test-key", "test-model");
	}

	@Test
	void summarizesValidJsonResponse() {
		mockServer.expect(requestTo(containsString("/chat/completions")))
				.andExpect(method(HttpMethod.POST))
				.andRespond(withSuccess(chatResponse("""
						{"summary": "Cliente interessado no plano anual", "decisions": ["Fechar em 30 dias"], \
						"nextSteps": [], "mentionedValues": ["R$ 5000"], "paymentMethod": "boleto", "objections": []}
						"""), MediaType.APPLICATION_JSON));

		SummaryContent content = provider.summarize("transcrição da reunião");

		assertThat(content.summary()).isEqualTo("Cliente interessado no plano anual");
		assertThat(content.decisions()).containsExactly("Fechar em 30 dias");
		assertThat(content.paymentMethod()).isEqualTo("boleto");
	}

	@Test
	void parsesResponseWrappedInMarkdownFence() {
		String fenced = "```json\n"
				+ "{\"summary\": \"Resumo\", \"decisions\": [], \"nextSteps\": [], \"mentionedValues\": [], "
				+ "\"paymentMethod\": null, \"objections\": []}\n"
				+ "```";
		mockServer.expect(requestTo(containsString("/chat/completions")))
				.andRespond(withSuccess(chatResponse(fenced), MediaType.APPLICATION_JSON));

		SummaryContent content = provider.summarize("transcrição");

		assertThat(content.summary()).isEqualTo("Resumo");
	}

	@Test
	void throwsWhenModelResponseIsNotValidJson() {
		mockServer.expect(requestTo(containsString("/chat/completions")))
				.andRespond(withSuccess(chatResponse("isso não é json"), MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> provider.summarize("transcrição"))
				.isInstanceOf(SummaryGenerationException.class);
	}

	@Test
	void throwsWhenSummaryFieldIsBlank() {
		mockServer.expect(requestTo(containsString("/chat/completions")))
				.andRespond(withSuccess(chatResponse("""
						{"summary": "", "decisions": [], "nextSteps": [], "mentionedValues": [], \
						"paymentMethod": null, "objections": []}
						"""), MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> provider.summarize("transcrição"))
				.isInstanceOf(SummaryGenerationException.class);
	}

	private String chatResponse(String content) {
		Map<String, Object> message = Map.of("role", "assistant", "content", content);
		Map<String, Object> choice = Map.of("message", message);
		Map<String, Object> body = Map.of("choices", List.of(choice));
		return objectMapper.writeValueAsString(body);
	}

}
