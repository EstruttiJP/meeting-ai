package com.meetingai.backend.aiprovider;

import java.util.UUID;

public class AiProviderConfigNotFoundException extends RuntimeException {

	public AiProviderConfigNotFoundException(UUID id) {
		super("Configuração de provider de IA não encontrada: " + id);
	}

}
