package com.meetingai.backend.summary;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import com.meetingai.backend.transcription.TranscriptionResult;
import com.meetingai.backend.transcription.TranscriptionSegment;

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
			Você é um assistente que ajuda a documentar reuniões. Analise a transcrição abaixo e devolva \
			SOMENTE um JSON válido (sem texto antes ou depois, sem blocos de código markdown), exatamente \
			com este formato:

			{
			  "summary": "resumo objetivo da reunião em um parágrafo",
			  "items": [
			    {"type": "decisao", "content": "o que foi decidido", "timestampSeconds": 12, "priority": "alta"},
			    {"type": "proximo_passo", "content": "o que ficou combinado fazer", "timestampSeconds": 45, "priority": "normal"},
			    {"type": "valor_mencionado", "content": "número, prazo ou valor citado", "timestampSeconds": 60},
			    {"type": "ponto_atencao", "content": "risco, dúvida ou objeção levantada", "timestampSeconds": 90}
			  ]
			}

			O campo "type" só aceita estes quatro valores: decisao, proximo_passo, valor_mencionado, \
			ponto_atencao. O campo "summary" é obrigatório e nunca pode ficar vazio. Se a reunião não tiver \
			nenhum item de um tipo, simplesmente não inclua itens daquele tipo — não invente conteúdo para \
			preencher.

			O campo "priority" ("alta" ou "normal") vale SOMENTE para decisao e proximo_passo; não inclua \
			esse campo nos outros dois tipos. Marque como "alta" apenas o que for realmente crítico ou \
			urgente para o andamento do trabalho — no máximo um ou dois itens da reunião inteira. Se nada \
			se destacar de verdade, marque tudo como "normal": é melhor não ter destaque do que eleger um \
			item qualquer.

			Cada linha da transcrição começa com o tempo em que ela foi dita, no formato [MM:SS]. Para cada \
			item extraído, "timestampSeconds" deve ser o tempo EM SEGUNDOS da linha onde aquilo foi dito — \
			converta [MM:SS] para segundos (ex.: [01:30] vira 90). Use sempre o tempo de uma linha que existe \
			na transcrição; nunca estime ou invente um tempo que não venha de uma linha.

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

	protected String buildPrompt(TranscriptionResult transcription) {
		return PROMPT_TEMPLATE.formatted(formatTranscript(transcription));
	}

	/**
	 * Sem segmentos (transcrição antiga, ou provider que não devolve tempo) o
	 * texto vai puro: o modelo continua extraindo itens, só não consegue ancorar
	 * no áudio, e os timestamps saem nulos na normalização.
	 */
	private static String formatTranscript(TranscriptionResult transcription) {
		if (transcription.segments().isEmpty()) {
			return transcription.content();
		}
		return transcription.segments().stream()
				.map(segment -> "[%s] %s".formatted(formatTimestamp(segment.startSeconds()), segment.text()))
				.collect(Collectors.joining("\n"));
	}

	private static String formatTimestamp(double seconds) {
		int total = (int) Math.round(seconds);
		return String.format(Locale.ROOT, "%02d:%02d", total / 60, total % 60);
	}

	protected SummaryContent parseAndValidate(String rawResponse, List<TranscriptionSegment> segments) {
		String json = extractJson(rawResponse);
		SummaryContent content;
		try {
			content = objectMapper.readValue(json, SummaryContent.class);
		} catch (JacksonException e) {
			throw new SummaryFormatException(
					"Resposta do modelo não é um JSON válido no formato esperado. Resposta bruta: "
							+ truncate(rawResponse), e);
		}
		content = normalize(content, segments);

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
	 * Ajusta o que o modelo não tem como garantir sozinho: id único por item
	 * (a edição granular depende disso) e timestamp ancorado num segmento que
	 * existe de verdade.
	 */
	private static SummaryContent normalize(SummaryContent content, List<TranscriptionSegment> segments) {
		List<SummaryItem> normalized = new ArrayList<>();
		Set<String> usedIds = new HashSet<>();
		int nextGeneratedId = 1;

		for (SummaryItem item : content.items()) {
			String id = item.id();
			if (id == null || id.isBlank() || !usedIds.add(id)) {
				while (!usedIds.add("item-" + nextGeneratedId)) {
					nextGeneratedId++;
				}
				id = "item-" + nextGeneratedId;
			}
			normalized.add(new SummaryItem(id, item.type(), item.content(),
					anchorToSegment(item.timestampSeconds(), segments),
					normalizePriority(item)));
		}
		return new SummaryContent(content.summary(), normalized);
	}

	/**
	 * Prioridade só sobrevive em decisão e próximo passo — nos outros tipos ela é
	 * descartada mesmo que o modelo tenha mandado. Ausente vira NORMAL, para o
	 * "é destaque?" ser sempre uma comparação simples, sem nulo no meio.
	 */
	private static SummaryItemPriority normalizePriority(SummaryItem item) {
		if (!item.canBeHighlighted()) {
			return null;
		}
		return item.priority() == null ? SummaryItemPriority.NORMAL : item.priority();
	}

	/**
	 * Reancora o tempo proposto pelo modelo no início do segmento mais próximo,
	 * garantindo que todo timestamp salvo veio de um trecho que existe de fato
	 * na gravação. Sem segmentos não há o que conferir, então o tempo é
	 * descartado — melhor item sem âncora do que âncora inventada.
	 */
	private static Double anchorToSegment(Double proposed, List<TranscriptionSegment> segments) {
		if (proposed == null || segments == null || segments.isEmpty()) {
			return null;
		}
		return segments.stream()
				.min(Comparator.comparingDouble(segment -> Math.abs(segment.startSeconds() - proposed)))
				.map(TranscriptionSegment::startSeconds)
				.orElse(null);
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
