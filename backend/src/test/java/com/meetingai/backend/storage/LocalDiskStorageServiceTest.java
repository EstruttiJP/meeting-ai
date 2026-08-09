package com.meetingai.backend.storage;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalDiskStorageServiceTest {

	@TempDir
	Path tempDir;

	private LocalDiskStorageService storageService;

	@BeforeEach
	void setUp() {
		storageService = new LocalDiskStorageService(tempDir.toString());
	}

	@Test
	void storesAndRetrievesFileContent() {
		String key = "user-1/meeting-1/audio.mp3";
		byte[] content = "conteudo do audio".getBytes(StandardCharsets.UTF_8);

		String storedKey = storageService.store(key, new ByteArrayInputStream(content), content.length, "audio/mpeg");

		assertThat(storedKey).isEqualTo(key);
		try (InputStream retrieved = storageService.retrieve(key)) {
			assertThat(retrieved.readAllBytes()).isEqualTo(content);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	void deleteRemovesTheFile() {
		String key = "user-1/meeting-2/audio.mp3";
		byte[] content = "conteudo".getBytes(StandardCharsets.UTF_8);
		storageService.store(key, new ByteArrayInputStream(content), content.length, "audio/mpeg");

		storageService.delete(key);

		assertThat(Files.exists(tempDir.resolve(key))).isFalse();
	}

	@Test
	void deleteOfMissingKeyDoesNotThrow() {
		assertThatCode(() -> storageService.delete("never-existed/audio.mp3")).doesNotThrowAnyException();
	}

	@Test
	void rejectsKeyThatEscapesBaseDirectoryViaTraversal() {
		assertThatThrownBy(() -> storageService.retrieve("../../etc/passwd"))
				.isInstanceOf(StorageException.class);
	}

}
