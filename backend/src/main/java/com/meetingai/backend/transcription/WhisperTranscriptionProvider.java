package com.meetingai.backend.transcription;

import java.io.InputStream;
import java.util.List;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Implementação de dev: chama o container local de Whisper
 * (onerahmet/openai-whisper-asr-webservice, ver docker-compose.yml) via HTTP.
 * Trocada pelo Amazon Transcribe na etapa de deploy.
 */
@Service
public class WhisperTranscriptionProvider implements TranscriptionProvider {

	public static final String PROVIDER_NAME = "whisper-local";

	private static final int RAW_RESPONSE_LOG_LIMIT = 500;

	private final RestClient restClient;
	private final ObjectMapper objectMapper;

	public WhisperTranscriptionProvider(RestClient whisperRestClient, ObjectMapper objectMapper) {
		this.restClient = whisperRestClient;
		this.objectMapper = objectMapper;
	}

	@Override
	public TranscriptionResult transcribe(InputStream audioContent, String filename, String contentType) {
		MultipartBodyBuilder body = new MultipartBodyBuilder();
		body.part("audio_file", new InputStreamResource(audioContent))
				.filename(filename)
				.contentType(MediaType.parseMediaType(contentType));

		String raw;
		try {
			raw = restClient.post()
					.uri(uriBuilder -> uriBuilder.path("/asr").queryParam("output", "json").build())
					.contentType(MediaType.MULTIPART_FORM_DATA)
					.body(body.build())
					.retrieve()
					.body(String.class);
		} catch (RestClientException e) {
			throw new TranscriptionException("Falha ao chamar o serviço de transcrição Whisper: " + e.getMessage(), e);
		}

		if (raw == null || raw.isBlank()) {
			throw new TranscriptionException("Resposta vazia do serviço de transcrição Whisper");
		}

		// O whisper-asr-webservice devolve JSON com Content-Type text/plain, o que
		// faz o conversor de mensagem do RestClient recusar a desserialização —
		// por isso o corpo é lido como texto e convertido aqui.
		WhisperResponse response;
		try {
			response = objectMapper.readValue(raw, WhisperResponse.class);
		} catch (JacksonException e) {
			throw new TranscriptionException(
					"Resposta do Whisper não é um JSON no formato esperado: " + truncate(raw), e);
		}

		if (response.text() == null || response.text().isBlank()) {
			throw new TranscriptionException(
					"Transcrição vazia devolvida pelo Whisper: " + truncate(raw));
		}
		return new TranscriptionResult(response.text().trim(), response.language(), toSegments(response.segments()));
	}

	@Override
	public String providerName() {
		return PROVIDER_NAME;
	}

	private static String truncate(String raw) {
		String flat = raw.strip().replaceAll("\\s+", " ");
		return flat.length() <= RAW_RESPONSE_LOG_LIMIT
				? flat
				: flat.substring(0, RAW_RESPONSE_LOG_LIMIT) + "...(truncado)";
	}

	/**
	 * Segmento sem texto é descartado: serve só como âncora de tempo para os
	 * itens do resumo, e âncora sem conteúdo não ajuda o modelo nem o player.
	 */
	private static List<TranscriptionSegment> toSegments(List<WhisperSegment> segments) {
		if (segments == null) {
			return List.of();
		}
		return segments.stream()
				.filter(s -> s.text() != null && !s.text().isBlank())
				.map(s -> new TranscriptionSegment(s.start(), s.end(), s.text().trim()))
				.toList();
	}

	private record WhisperSegment(double start, double end, String text) {
	}

	private record WhisperResponse(String text, String language, List<WhisperSegment> segments) {
	}

}
