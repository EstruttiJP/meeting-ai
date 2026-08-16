package com.meetingai.backend.summary;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Um item extraído da reunião, ancorado no ponto da gravação em que foi dito.
 *
 * <p>O {@code id} é o que permite editar, adicionar ou remover um item sem
 * mexer nos outros — sem ele a revisão voltaria a ser "substitui tudo".
 *
 * <p>{@code timestampSeconds} é nulo quando não foi possível ancorar o item
 * num segmento real do transcript. Preferimos assumir a ausência a inventar um
 * tempo que mandaria o player para o lugar errado.
 */
public record SummaryItem(
		@NotBlank String id,
		@NotNull SummaryItemType type,
		@NotBlank String content,
		@PositiveOrZero Double timestampSeconds) {
}
