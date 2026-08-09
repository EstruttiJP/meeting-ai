package com.meetingai.backend.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;

/**
 * Criptografa/decriptografa segredos antes de persistir (chave própria de IA
 * do usuário, tokens OAuth do CRM). Nunca logar o texto puro nem devolver em
 * resposta de API — quem chama isso é responsável por essas duas regras.
 */
@Service
public class EncryptionService {

	private final TextEncryptor encryptor;

	public EncryptionService(
			@Value("${app.encryption.key}") String key,
			@Value("${app.encryption.salt}") String salt) {
		this.encryptor = Encryptors.text(key, salt);
	}

	public String encrypt(String plainText) {
		if (plainText == null) {
			return null;
		}
		return encryptor.encrypt(plainText);
	}

	public String decrypt(String cipherText) {
		if (cipherText == null) {
			return null;
		}
		return encryptor.decrypt(cipherText);
	}

}
