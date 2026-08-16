package com.meetingai.backend.summary;

/**
 * O modelo respondeu, mas fora do JSON esperado por {@link SummaryContent}.
 * Separado de {@link SummaryGenerationException} porque a causa é diferente
 * (prompt/modelo, não indisponibilidade da API) e o usuário merece uma
 * mensagem diferente — tentar de novo costuma resolver.
 */
public class SummaryFormatException extends SummaryGenerationException {

	public SummaryFormatException(String message) {
		super(message);
	}

	public SummaryFormatException(String message, Throwable cause) {
		super(message, cause);
	}

}
