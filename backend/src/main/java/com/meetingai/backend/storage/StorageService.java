package com.meetingai.backend.storage;

import java.io.InputStream;

/**
 * Abstrai onde os arquivos de reunião ficam gravados. Nenhuma lógica de
 * negócio deve depender de {@code java.io.File}/S3/etc diretamente — tudo
 * passa por aqui, para que a implementação possa ser trocada só por
 * configuração (disco local em dev, S3 em produção).
 */
public interface StorageService {

	/**
	 * Grava o conteúdo sob a chave sugerida e devolve a chave efetivamente usada.
	 */
	String store(String suggestedKey, InputStream content, long contentLength, String contentType);

	InputStream retrieve(String key);

	void delete(String key);

}
