package com.meetingai.backend.summary;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Peso de um item na reunião. Só faz sentido em decisão e próximo passo — são
 * os únicos que competem pelo card de destaque da aba Resumo. Ponto de atenção
 * e valor mencionado não recebem prioridade: marcar "valor alto" como prioridade
 * alta confundiria criticidade com magnitude.
 */
public enum SummaryItemPriority {

	ALTA("alta"),
	NORMAL("normal");

	private final String jsonValue;

	SummaryItemPriority(String jsonValue) {
		this.jsonValue = jsonValue;
	}

	@JsonValue
	public String jsonValue() {
		return jsonValue;
	}

	/** Tolerante na entrada pelo mesmo motivo de {@link SummaryItemType}: quem preenche é um LLM. */
	@JsonCreator
	public static SummaryItemPriority fromJson(String value) {
		if (value == null) {
			return null;
		}
		String normalized = value.trim().toLowerCase();
		for (SummaryItemPriority priority : values()) {
			if (priority.jsonValue.equals(normalized)) {
				return priority;
			}
		}
		throw new IllegalArgumentException("Prioridade desconhecida: " + value);
	}

}
