package com.meetingai.backend.summary;

/**
 * Falha ao gerar ou validar o resumo estruturado — chamada do LLM falhou,
 * ou a resposta não veio no formato esperado. Quem chama trata isso como
 * falha do pipeline (status FAILED), nunca salva o dado malformado.
 */
public class SummaryGenerationException extends RuntimeException {

	public SummaryGenerationException(String message) {
		super(message);
	}

	public SummaryGenerationException(String message, Throwable cause) {
		super(message, cause);
	}

}
