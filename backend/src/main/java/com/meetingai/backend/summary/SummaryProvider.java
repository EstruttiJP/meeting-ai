package com.meetingai.backend.summary;

/**
 * Gera o resumo estruturado a partir da transcrição, usando o provider
 * padrão (mantido pela própria aplicação, hoje OpenRouter no plano free).
 * Para a chave própria do usuário, ver {@link UserKeySummaryProvider}.
 */
public interface SummaryProvider {

	SummaryContent summarize(String transcriptionText);

}
