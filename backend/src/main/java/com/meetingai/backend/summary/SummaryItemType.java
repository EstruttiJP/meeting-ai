package com.meetingai.backend.summary;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Tipo de item extraído da reunião. Os rótulos são propositalmente neutros:
 * o produto é um facilitador de reunião, então "ponto de atenção" cobre o que
 * antes era chamado de "objeção" sem assumir que toda reunião é uma venda.
 */
public enum SummaryItemType {

	DECISAO("decisao"),
	PROXIMO_PASSO("proximo_passo"),
	VALOR_MENCIONADO("valor_mencionado"),
	PONTO_ATENCAO("ponto_atencao");

	private final String jsonValue;

	SummaryItemType(String jsonValue) {
		this.jsonValue = jsonValue;
	}

	@JsonValue
	public String jsonValue() {
		return jsonValue;
	}

	/**
	 * Tolerante de propósito na entrada: o valor vem de um LLM, que erra
	 * maiúscula/minúscula e acento com frequência. Só o formato de saída é
	 * rígido.
	 */
	@JsonCreator
	public static SummaryItemType fromJson(String value) {
		if (value == null) {
			return null;
		}
		String normalized = value.trim().toLowerCase();
		for (SummaryItemType type : values()) {
			if (type.jsonValue.equals(normalized) || type.name().toLowerCase().equals(normalized)) {
				return type;
			}
		}
		throw new IllegalArgumentException("Tipo de item desconhecido: " + value);
	}

}
