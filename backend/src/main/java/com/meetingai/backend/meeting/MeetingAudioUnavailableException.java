package com.meetingai.backend.meeting;

/**
 * O áudio original não está mais disponível — expirou e foi apagado pela
 * retenção. É um limite esperado do produto, não um erro: a tela de revisão
 * usa isso para explicar por que não há player, em vez de mostrar um player
 * quebrado.
 */
public class MeetingAudioUnavailableException extends RuntimeException {

	public MeetingAudioUnavailableException(String message) {
		super(message);
	}

}
