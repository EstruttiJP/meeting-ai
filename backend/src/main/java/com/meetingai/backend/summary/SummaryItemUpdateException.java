package com.meetingai.backend.summary;

/** Edição de item que não faz sentido (nada a alterar, texto vazio, prioridade em tipo que não tem). */
public class SummaryItemUpdateException extends RuntimeException {

	public SummaryItemUpdateException(String message) {
		super(message);
	}

}
