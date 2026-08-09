package com.meetingai.backend.transcription;

import java.io.InputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Implementação de dev: chama o container local de Whisper
 * (onerahmet/openai-whisper-asr-webservice, ver docker-compose.yml) via HTTP.
 * Trocada pelo Amazon Transcribe na etapa de deploy.
 */
@Service
public class WhisperTranscriptionProvider implements TranscriptionProvider {

	public static final String PROVIDER_NAME = "whisper-local";

	private final RestClient restClient;

	public WhisperTranscriptionProvider(RestClient.Builder restClientBuilder,
			@Value("${app.transcription.whisper.base-url}") String baseUrl) {
		this.restClient = restClientBuilder.baseUrl(baseUrl).build();
	}

	@Override
	public TranscriptionResult transcribe(InputStream audioContent, String filename, String contentType) {
		MultipartBodyBuilder body = new MultipartBodyBuilder();
		body.part("audio_file", new InputStreamResource(audioContent))
				.filename(filename)
				.contentType(MediaType.parseMediaType(contentType));

		WhisperResponse response;
		try {
			response = restClient.post()
					.uri(uriBuilder -> uriBuilder.path("/asr").queryParam("output", "json").build())
					.contentType(MediaType.MULTIPART_FORM_DATA)
					.body(body.build())
					.retrieve()
					.body(WhisperResponse.class);
		} catch (RestClientException e) {
			throw new TranscriptionException("Falha ao chamar o serviço de transcrição Whisper", e);
		}

		if (response == null || response.text() == null || response.text().isBlank()) {
			throw new TranscriptionException("Resposta vazia do serviço de transcrição Whisper");
		}
		return new TranscriptionResult(response.text().trim(), response.language());
	}

	@Override
	public String providerName() {
		return PROVIDER_NAME;
	}

	private record WhisperResponse(String text, String language) {
	}

}
