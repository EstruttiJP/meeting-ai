package com.meetingai.backend.summary;

import jakarta.validation.constraints.NotBlank;

/** Edição só do texto corrido do resumo, sem tocar na lista de itens. */
public record SummaryTextUpdateRequest(@NotBlank String summary) {
}
