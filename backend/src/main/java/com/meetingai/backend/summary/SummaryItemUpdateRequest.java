package com.meetingai.backend.summary;

import jakarta.validation.constraints.NotBlank;

/**
 * Correção do texto de um item já existente. Tipo e timestamp não mudam na
 * edição: o timestamp está ancorado num trecho real do áudio, e reescrevê-lo
 * a partir do formulário quebraria essa garantia.
 */
public record SummaryItemUpdateRequest(@NotBlank String content) {
}
