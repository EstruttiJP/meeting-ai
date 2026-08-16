package com.meetingai.backend.summary;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Formato do resumo revisado: um texto corrido mais uma lista plana de itens
 * tipados. É plana (e não uma lista por tipo) porque a tela de revisão mostra
 * tudo em ordem cronológica, acompanhando o áudio — agrupar por tipo quebraria
 * essa leitura.
 */
public record SummaryContent(
		@NotBlank String summary,
		@NotNull @Valid List<SummaryItem> items) {

	public SummaryContent {
		items = items == null ? List.of() : List.copyOf(items);
	}

	public List<SummaryItem> itemsOfType(SummaryItemType type) {
		return items.stream().filter(item -> item.type() == type).toList();
	}

}
