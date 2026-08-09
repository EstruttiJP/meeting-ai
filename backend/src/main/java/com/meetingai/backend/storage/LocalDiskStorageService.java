package com.meetingai.backend.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Implementação de dev: grava os arquivos numa pasta local (fora do
 * controle de versão), configurável via {@code STORAGE_LOCAL_PATH}. Trocada
 * por uma implementação em S3 na etapa de deploy, sem mudar quem consome
 * {@link StorageService}.
 */
@Service
public class LocalDiskStorageService implements StorageService {

	private final Path basePath;

	public LocalDiskStorageService(@Value("${app.storage.local.base-path}") String basePath) {
		this.basePath = Path.of(basePath).toAbsolutePath().normalize();
		try {
			Files.createDirectories(this.basePath);
		} catch (IOException e) {
			throw new StorageException("Não foi possível criar o diretório de armazenamento local: " + basePath, e);
		}
	}

	@Override
	public String store(String suggestedKey, InputStream content, long contentLength, String contentType) {
		Path target = resolve(suggestedKey);
		try {
			Files.createDirectories(target.getParent());
			Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			throw new StorageException("Falha ao gravar arquivo em armazenamento local: " + suggestedKey, e);
		}
		return suggestedKey;
	}

	@Override
	public InputStream retrieve(String key) {
		try {
			return Files.newInputStream(resolve(key));
		} catch (IOException e) {
			throw new StorageException("Falha ao ler arquivo do armazenamento local: " + key, e);
		}
	}

	@Override
	public void delete(String key) {
		try {
			Files.deleteIfExists(resolve(key));
		} catch (IOException e) {
			throw new StorageException("Falha ao apagar arquivo do armazenamento local: " + key, e);
		}
	}

	private Path resolve(String key) {
		Path resolved = basePath.resolve(key).normalize();
		if (!resolved.startsWith(basePath)) {
			throw new StorageException("Chave de armazenamento inválida: " + key);
		}
		return resolved;
	}

}
