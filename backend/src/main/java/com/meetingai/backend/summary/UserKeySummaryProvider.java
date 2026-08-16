package com.meetingai.backend.summary;

import com.meetingai.backend.aiprovider.AiProvider;
import com.meetingai.backend.meeting.MeetingType;
import com.meetingai.backend.transcription.TranscriptionResult;

/**
 * Gera o resumo estruturado usando a chave de API que o próprio usuário
 * colou nas configurações (Gemini, GPT ou Claude), em vez do provider padrão
 * da aplicação. Uma implementação por {@link AiProvider} suportado.
 */
public interface UserKeySummaryProvider {

	AiProvider supportedProvider();

	SummaryContent summarize(TranscriptionResult transcription, MeetingType meetingType, String apiKey);

}
