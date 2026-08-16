package com.meetingai.backend.summary;

/**
 * Edição de um item já existente. Os dois campos são opcionais e independentes:
 * a tela usa esse mesmo endpoint para corrigir o texto e para promover/rebaixar
 * o item no card de destaque, sem que uma coisa exija mandar a outra.
 *
 * <p>Tipo e timestamp continuam imutáveis: o timestamp está ancorado num trecho
 * real do áudio, e reescrevê-lo pelo formulário quebraria essa garantia.
 */
public record SummaryItemUpdateRequest(String content, SummaryItemPriority priority) {

	public boolean isEmpty() {
		return content == null && priority == null;
	}

}
