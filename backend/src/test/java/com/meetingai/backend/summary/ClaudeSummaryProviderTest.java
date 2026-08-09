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

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ClaudeSummaryProviderTest {

	private final ObjectMapper objectMapper = JsonMapper.builder().build();
	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	private MockRestServiceServer mockServer;
	private ClaudeSummaryProvider provider;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		mockServer = MockRestServiceServer.bindTo(builder).build();
		provider = new ClaudeSummaryProvider(builder, objectMapper, validator,
				"https://claude.local/v1", "claude-test-model");
	}

	@Test
	void reportsClaudeAsSupportedProvider() {
		assertThat(provider.supportedProvider()).isEqualTo(AiProvider.CLAUDE);
	}

	@Test
	void summarizesValidJsonResponseAndSendsApiKeyHeader() {
		String innerJson = """
				{"summary": "Reunião de fechamento", "decisions": ["Assinar contrato"], "nextSteps": [], \
				"mentionedValues": ["US$ 10000"], "paymentMethod": "cartão", "objections": []}
				""";
		mockServer.expect(requestTo(containsString("/messages")))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header("x-api-key", "user-api-key"))
				.andRespond(withSuccess(claudeResponse(innerJson), MediaType.APPLICATION_JSON));

		SummaryContent content = provider.summarize("transcrição", "user-api-key");

		assertThat(content.summary()).isEqualTo("Reunião de fechamento");
		assertThat(content.decisions()).containsExactly("Assinar contrato");
		assertThat(content.paymentMethod()).isEqualTo("cartão");
	}

	private String claudeResponse(String text) {
		Map<String, Object> block = Map.of("type", "text", "text", text);
		Map<String, Object> body = Map.of("content", List.of(block));
		return objectMapper.writeValueAsString(body);
	}

}
