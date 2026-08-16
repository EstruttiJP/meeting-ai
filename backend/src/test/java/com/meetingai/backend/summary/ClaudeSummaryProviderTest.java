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

	private static final TranscriptionResult TRANSCRICAO = new TranscriptionResult(
			"texto completo", "pt",
			List.of(new TranscriptionSegment(0, 8, "Abertura"),
					new TranscriptionSegment(30, 38, "Meio da conversa")));

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
				{"summary": "Reunião de fechamento", "items": [
				  {"type": "decisao", "content": "Assinar contrato", "timestampSeconds": 30},
				  {"type": "valor_mencionado", "content": "US$ 10000", "timestampSeconds": 0}]}
				""";
		mockServer.expect(requestTo(containsString("/messages")))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header("x-api-key", "user-api-key"))
				.andRespond(withSuccess(claudeResponse(innerJson), MediaType.APPLICATION_JSON));

		SummaryContent content = provider.summarize(TRANSCRICAO, "user-api-key");

		assertThat(content.summary()).isEqualTo("Reunião de fechamento");
		assertThat(content.itemsOfType(SummaryItemType.DECISAO))
				.singleElement().satisfies(item -> assertThat(item.content()).isEqualTo("Assinar contrato"));
		assertThat(content.itemsOfType(SummaryItemType.VALOR_MENCIONADO))
				.singleElement().satisfies(item -> assertThat(item.content()).isEqualTo("US$ 10000"));
	}

	private String claudeResponse(String text) {
		Map<String, Object> block = Map.of("type", "text", "text", text);
		Map<String, Object> body = Map.of("content", List.of(block));
		return objectMapper.writeValueAsString(body);
	}

}
