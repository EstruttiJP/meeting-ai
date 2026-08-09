package com.meetingai.backend.summary;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.meetingai.backend.aiprovider.AiProvider;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenAiSummaryProviderTest {

	private final ObjectMapper objectMapper = JsonMapper.builder().build();
	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	private MockRestServiceServer mockServer;
	private OpenAiSummaryProvider provider;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		mockServer = MockRestServiceServer.bindTo(builder).build();
		provider = new OpenAiSummaryProvider(builder, objectMapper, validator,
				"https://openai.local/v1", "gpt-test-model");
	}

	@Test
	void reportsOpenAiAsSupportedProvider() {
		assertThat(provider.supportedProvider()).isEqualTo(AiProvider.OPENAI);
	}

	@Test
	void summarizesValidJsonResponse() {
		Map<String, Object> message = Map.of("role", "assistant", "content", """
				{"summary": "Follow-up agendado", "decisions": [], "nextSteps": [], "mentionedValues": [], \
				"paymentMethod": null, "objections": []}
				""");
		Map<String, Object> body = Map.of("choices", List.of(Map.of("message", message)));
		mockServer.expect(requestTo(containsString("/chat/completions")))
				.andRespond(withSuccess(objectMapper.writeValueAsString(body), MediaType.APPLICATION_JSON));

		SummaryContent content = provider.summarize("transcrição", "user-api-key");

		assertThat(content.summary()).isEqualTo("Follow-up agendado");
	}

}
