package com.meetingai.backend.summary;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.meetingai.backend.transcription.TranscriptionResult;
import com.meetingai.backend.transcription.TranscriptionSegment;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenRouterSummaryProviderTest {

	private final ObjectMapper objectMapper = JsonMapper.builder().build();
	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	private MockRestServiceServer mockServer;
	private OpenRouterSummaryProvider provider;

	private static final TranscriptionResult TRANSCRIPTION = new TranscriptionResult(
			"texto completo", "pt",
			List.of(new TranscriptionSegment(0, 8, "Abertura da reunião"),
					new TranscriptionSegment(30, 38, "Fechamos o plano anual"),
					new TranscriptionSegment(90, 97, "O prazo preocupa")));

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
						{"summary": "Reunião de alinhamento", "items": [
						  {"type": "decisao", "content": "Fechar o plano anual", "timestampSeconds": 30},
						  {"type": "ponto_atencao", "content": "Prazo apertado", "timestampSeconds": 90}
						]}
						"""), MediaType.APPLICATION_JSON));

		SummaryContent content = provider.summarize(TRANSCRIPTION);

		assertThat(content.summary()).isEqualTo("Reunião de alinhamento");
		assertThat(content.items()).hasSize(2);
		assertThat(content.itemsOfType(SummaryItemType.DECISAO))
				.singleElement()
				.satisfies(item -> {
					assertThat(item.content()).isEqualTo("Fechar o plano anual");
					assertThat(item.timestampSeconds()).isEqualTo(30.0);
				});
	}

	@Test
	void sendsTheTranscriptMarkedWithTimestamps() {
		mockServer.expect(requestTo(containsString("/chat/completions")))
				.andExpect(content().string(containsString("[00:30] Fechamos o plano anual")))
				.andRespond(withSuccess(chatResponse("""
						{"summary": "Resumo", "items": []}
						"""), MediaType.APPLICATION_JSON));

		provider.summarize(TRANSCRIPTION);

		mockServer.verify();
	}

	@Test
	void reanchorsTimestampsThatDoNotMatchAnySegment() {
		mockServer.expect(requestTo(containsString("/chat/completions")))
				.andRespond(withSuccess(chatResponse("""
						{"summary": "Resumo", "items": [
						  {"type": "decisao", "content": "Inventado", "timestampSeconds": 5000}
						]}
						"""), MediaType.APPLICATION_JSON));

		SummaryContent content = provider.summarize(TRANSCRIPTION);

		// 5000 não existe na gravação; o item é ancorado no segmento mais próximo.
		assertThat(content.items()).singleElement()
				.satisfies(item -> assertThat(item.timestampSeconds()).isEqualTo(90.0));
	}

	@Test
	void dropsTimestampsWhenTranscriptHasNoSegments() {
		mockServer.expect(requestTo(containsString("/chat/completions")))
				.andRespond(withSuccess(chatResponse("""
						{"summary": "Resumo", "items": [
						  {"type": "decisao", "content": "Sem âncora possível", "timestampSeconds": 42}
						]}
						"""), MediaType.APPLICATION_JSON));

		SummaryContent content = provider.summarize(new TranscriptionResult("texto", "pt", List.of()));

		assertThat(content.items()).singleElement()
				.satisfies(item -> assertThat(item.timestampSeconds()).isNull());
	}

	@Test
	void generatesUniqueIdsSoItemsCanBeEditedIndividually() {
		mockServer.expect(requestTo(containsString("/chat/completions")))
				.andRespond(withSuccess(chatResponse("""
						{"summary": "Resumo", "items": [
						  {"id": "x", "type": "decisao", "content": "Primeira", "timestampSeconds": 0},
						  {"id": "x", "type": "decisao", "content": "Segunda", "timestampSeconds": 30}
						]}
						"""), MediaType.APPLICATION_JSON));

		SummaryContent content = provider.summarize(TRANSCRIPTION);

		assertThat(content.items()).map(SummaryItem::id).doesNotHaveDuplicates();
	}

	@Test
	void parsesResponseWrappedInMarkdownFence() {
		String fenced = "```json\n{\"summary\": \"Resumo\", \"items\": []}\n```";
		mockServer.expect(requestTo(containsString("/chat/completions")))
				.andRespond(withSuccess(chatResponse(fenced), MediaType.APPLICATION_JSON));

		SummaryContent content = provider.summarize(TRANSCRIPTION);

		assertThat(content.summary()).isEqualTo("Resumo");
	}

	@Test
	void throwsWhenModelResponseIsNotValidJson() {
		mockServer.expect(requestTo(containsString("/chat/completions")))
				.andRespond(withSuccess(chatResponse("isso não é json"), MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> provider.summarize(TRANSCRIPTION))
				.isInstanceOf(SummaryFormatException.class);
	}

	@Test
	void throwsWhenSummaryFieldIsBlank() {
		mockServer.expect(requestTo(containsString("/chat/completions")))
				.andRespond(withSuccess(chatResponse("""
						{"summary": "", "items": []}
						"""), MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> provider.summarize(TRANSCRIPTION))
				.isInstanceOf(SummaryFormatException.class);
	}

	@Test
	void throwsWhenItemTypeIsUnknown() {
		mockServer.expect(requestTo(containsString("/chat/completions")))
				.andRespond(withSuccess(chatResponse("""
						{"summary": "Resumo", "items": [
						  {"type": "forma_de_pagamento", "content": "boleto", "timestampSeconds": 0}
						]}
						"""), MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> provider.summarize(TRANSCRIPTION))
				.isInstanceOf(SummaryFormatException.class);
	}

	private String chatResponse(String content) {
		Map<String, Object> message = Map.of("role", "assistant", "content", content);
		Map<String, Object> choice = Map.of("message", message);
		Map<String, Object> body = Map.of("choices", List.of(choice));
		return objectMapper.writeValueAsString(body);
	}

}
