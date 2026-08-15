package com.meetingai.backend.summary;

import java.util.Set;
import java.util.stream.Collectors;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

/**
 * Monta o prompt de extração estruturada e valida a resposta do modelo
 * contra o schema de {@link SummaryContent} antes de devolver — compartilhado
 * por todas as implementações de {@link SummaryProvider}/{@link UserKeySummaryProvider},
 * já que só muda quem chama qual API, não o formato pedido nem a validação.
 */
abstract class AbstractLlmSummaryProvider {

	private static final String PROMPT_TEMPLATE = """
			Você é um assistente que extrai informações estruturadas de transcrições de reuniões comerciais.
			Analise a transcrição abaixo e devolva SOMENTE um JSON válido (sem texto antes ou depois, sem blocos \
			de código markdown), exatamente com este formato:

			{
			  "summary": "resumo objetivo da reunião em um parágrafo",
			  "decisions": ["decisão 1", "decisão 2"],
			  "nextSteps": ["próximo passo 1", "próximo passo 2"],
			  "mentionedValues": ["valor mencionado 1", "valor mencionado 2"],
			  "paymentMethod": "forma de pagamento citada, ou null se não houver",
			  "objections": ["objeção 1", "objeção 2"]
			}

			Se uma lista não tiver itens, devolva uma lista vazia []. O campo "summary" é obrigatório e nunca \
			pode ficar vazio.

			Transcrição:
			%s
			""";

	private static final int RAW_RESPONSE_LOG_LIMIT = 500;

	private final ObjectMapper objectMapper;
	private final Validator validator;

	protected AbstractLlmSummaryProvider(ObjectMapper objectMapper, Validator validator) {
		this.objectMapper = objectMapper;
		this.validator = validator;
	}

	protected String buildPrompt(String transcriptionText) {
		return PROMPT_TEMPLATE.formatted(transcriptionText);
	}

	protected SummaryContent parseAndValidate(String rawResponse) {
		String json = extractJson(rawResponse);
		SummaryContent content;
		try {
			content = objectMapper.readValue(json, SummaryContent.class);
		} catch (JacksonException e) {
			throw new SummaryFormatException(
					"Resposta do modelo não é um JSON válido no formato esperado. Resposta bruta: "
							+ truncate(rawResponse), e);
		}
		Set<ConstraintViolation<SummaryContent>> violations = validator.validate(content);
		if (!violations.isEmpty()) {
			String campos = violations.stream()
					.map(v -> v.getPropertyPath() + " " + v.getMessage())
					.collect(Collectors.joining("; "));
			throw new SummaryFormatException(
					"Resposta do modelo não atende ao schema esperado (" + campos + "). Resposta bruta: "
							+ truncate(rawResponse));
		}
		return content;
	}

	/**
	 * Trunca porque a resposta bruta vai parar no log e em
	 * {@code meeting.failure_reason} — o suficiente para diagnosticar o formato
	 * sem despejar a transcrição inteira.
	 */
	private static String truncate(String rawResponse) {
		String flat = rawResponse.strip().replaceAll("\\s+", " ");
		return flat.length() <= RAW_RESPONSE_LOG_LIMIT
				? flat
				: flat.substring(0, RAW_RESPONSE_LOG_LIMIT) + "...(truncado)";
	}

	private String extractJson(String rawResponse) {
		if (rawResponse == null || rawResponse.isBlank()) {
			throw new SummaryFormatException("Resposta vazia do modelo de IA");
		}
		String trimmed = rawResponse.trim();
		if (trimmed.startsWith("```")) {
			int firstNewline = trimmed.indexOf('\n');
			int lastFence = trimmed.lastIndexOf("```");
			if (firstNewline != -1 && lastFence > firstNewline) {
				trimmed = trimmed.substring(firstNewline + 1, lastFence).trim();
			}
		}
		return trimmed;
	}

}
