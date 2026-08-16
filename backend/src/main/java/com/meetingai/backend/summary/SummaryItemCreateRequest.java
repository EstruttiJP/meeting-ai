package com.meetingai.backend.summary;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Item novo adicionado à mão na revisão. O id não vem do cliente: quem gera é
 * o servidor, para não haver colisão com os ids que o modelo produziu.
 */
public record SummaryItemCreateRequest(
		@NotNull SummaryItemType type,
		@NotBlank String content,
		@PositiveOrZero Double timestampSeconds) {
}
