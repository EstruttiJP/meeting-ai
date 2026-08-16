package com.meetingai.backend.summary;

/**
 * Item inexistente no resumo — normalmente uma tela desatualizada tentando
 * editar algo que outra aba já removeu.
 */
public class SummaryItemNotFoundException extends RuntimeException {

	public SummaryItemNotFoundException(String itemId) {
		super("Item de resumo não encontrado: " + itemId);
	}

}
