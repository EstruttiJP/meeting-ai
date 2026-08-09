package com.meetingai.backend.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EncryptionServiceTest {

	private EncryptionService encryptionService;

	@BeforeEach
	void setUp() {
		encryptionService = new EncryptionService("test-encryption-key", "deadbeef");
	}

	@Test
	void encryptsAndDecryptsRoundTrip() {
		String plainText = "sk-super-secret-api-key";

		String cipherText = encryptionService.encrypt(plainText);

		assertThat(cipherText).isNotEqualTo(plainText);
		assertThat(encryptionService.decrypt(cipherText)).isEqualTo(plainText);
	}

	@Test
	void sameInputProducesDifferentCipherTextEachTime() {
		String plainText = "sk-super-secret-api-key";

		String first = encryptionService.encrypt(plainText);
		String second = encryptionService.encrypt(plainText);

		assertThat(first).isNotEqualTo(second);
	}

	@Test
	void handlesNullValuesWithoutThrowing() {
		assertThat(encryptionService.encrypt(null)).isNull();
		assertThat(encryptionService.decrypt(null)).isNull();
	}

	@Test
	void decryptingTamperedCipherTextFails() {
		String cipherText = encryptionService.encrypt("valor original");

		assertThatThrownBy(() -> encryptionService.decrypt(cipherText + "ff"))
				.isInstanceOf(RuntimeException.class);
	}

}
