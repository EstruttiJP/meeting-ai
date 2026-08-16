package com.meetingai.backend.meeting;

/**
 * Etapa do pipeline em que a reunião falhou. Serve para o frontend mostrar uma
 * mensagem específica sem expor o motivo técnico (que fica em
 * {@code meeting.failure_reason}, para diagnóstico).
 */
public enum MeetingFailureCategory {

	/** Falha ao ler o áudio ou ao chamar o serviço de transcrição. */
	TRANSCRIPTION,

	/** Falha ao chamar o provider de IA que gera o resumo. */
	SUMMARY,

	/** O modelo respondeu, mas fora do schema JSON esperado. */
	INVALID_SUMMARY_FORMAT,

	/** Qualquer falha não prevista — sempre acompanhada do motivo técnico. */
	UNKNOWN
}
