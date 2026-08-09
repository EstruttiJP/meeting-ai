package com.meetingai.backend.summary;

import com.meetingai.backend.aiprovider.AiProvider;

/**
 * Gera o resumo estruturado usando a chave de API que o próprio usuário
 * colou nas configurações (Gemini, GPT ou Claude), em vez do provider padrão
 * da aplicação. Uma implementação por {@link AiProvider} suportado.
 */
public interface UserKeySummaryProvider {

	AiProvider supportedProvider();

	SummaryContent summarize(String transcriptionText, String apiKey);

}
