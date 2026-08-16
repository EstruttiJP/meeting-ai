package com.meetingai.backend.meeting;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.meetingai.backend.meeting.MeetingPipelineSteps.PipelineContext;
import com.meetingai.backend.storage.StorageService;
import com.meetingai.backend.summary.SummaryContent;
import com.meetingai.backend.summary.SummaryFormatException;
import com.meetingai.backend.summary.SummaryGenerationException;
import com.meetingai.backend.summary.SummaryOrchestrator;
import com.meetingai.backend.transcription.TranscriptionException;
import com.meetingai.backend.transcription.TranscriptionProvider;
import com.meetingai.backend.transcription.TranscriptionResult;
import com.meetingai.backend.user.User;
import com.meetingai.backend.user.UserRepository;

/**
 * Dispara e conduz o pipeline assíncrono (transcrição + resumo) depois que o
 * upload já foi aceito e persistido — a resposta HTTP do upload não espera
 * isso terminar. Qualquer falha em qualquer passo marca a reunião como
 * FAILED em vez de deixar status inconsistente ou dado parcial salvo.
 */
@Service
public class MeetingPipelineService {

	private static final Logger log = LoggerFactory.getLogger(MeetingPipelineService.class);

	private static final int REASON_MAX_LENGTH = 2000;

	private static final Map<String, String> CONTENT_TYPES_BY_EXTENSION = Map.of(
			"mp3", "audio/mpeg",
			"mp4", "video/mp4",
			"wav", "audio/wav",
			"m4a", "audio/mp4");

	private final MeetingPipelineSteps steps;
	private final StorageService storageService;
	private final TranscriptionProvider transcriptionProvider;
	private final SummaryOrchestrator summaryOrchestrator;
	private final UserRepository userRepository;

	public MeetingPipelineService(MeetingPipelineSteps steps,
			StorageService storageService,
			TranscriptionProvider transcriptionProvider,
			SummaryOrchestrator summaryOrchestrator,
			UserRepository userRepository) {
		this.steps = steps;
		this.storageService = storageService;
		this.transcriptionProvider = transcriptionProvider;
		this.summaryOrchestrator = summaryOrchestrator;
		this.userRepository = userRepository;
	}

	@Async
	public void process(UUID meetingId) {
		try {
			runPipeline(meetingId);
		} catch (Exception e) {
			MeetingFailureCategory category = categorize(e);
			String reason = describe(e);
			log.error("Pipeline falhou para a reunião {} na etapa {}: {}", meetingId, category, reason, e);
			steps.markFailed(meetingId, category, reason);
		}
	}

	private void runPipeline(UUID meetingId) {
		PipelineContext context = steps.markTranscribing(meetingId);

		log.info("Transcrevendo reunião {} (arquivo {}) via provider {}",
				meetingId, context.originalFilename(), transcriptionProvider.providerName());
		TranscriptionResult transcriptionResult;
		try (InputStream audio = storageService.retrieve(context.storageKey())) {
			transcriptionResult = transcriptionProvider.transcribe(
					audio, context.originalFilename(), guessContentType(context.originalFilename()));
		} catch (IOException e) {
			throw new TranscriptionException("Falha ao ler o arquivo de áudio para transcrição", e);
		}
		steps.saveTranscriptionAndMarkSummarizing(meetingId, transcriptionResult, transcriptionProvider.providerName());
		log.info("Reunião {} transcrita: {} caracteres, idioma {}",
				meetingId, transcriptionResult.content().length(), transcriptionResult.language());

		User user = userRepository.findById(context.userId())
				.orElseThrow(() -> new IllegalStateException("Usuário da reunião não encontrado: " + context.userId()));
		SummaryContent summaryContent = summaryOrchestrator.summarize(user, transcriptionResult, context.meetingType());
		steps.saveSummaryAndMarkReady(meetingId, summaryContent);
		log.info("Reunião {} resumida e marcada como READY", meetingId);
	}

	private static MeetingFailureCategory categorize(Exception e) {
		if (e instanceof TranscriptionException) {
			return MeetingFailureCategory.TRANSCRIPTION;
		}
		if (e instanceof SummaryFormatException) {
			return MeetingFailureCategory.INVALID_SUMMARY_FORMAT;
		}
		if (e instanceof SummaryGenerationException) {
			return MeetingFailureCategory.SUMMARY;
		}
		return MeetingFailureCategory.UNKNOWN;
	}

	/**
	 * Mensagem técnica com a cadeia de causas — sem isso o motivo gravado vira
	 * "Falha ao chamar a API de IA" sem dizer que por baixo houve um 402.
	 */
	private static String describe(Throwable e) {
		StringBuilder text = new StringBuilder();
		for (Throwable current = e; current != null && text.length() < REASON_MAX_LENGTH; current = current.getCause()) {
			if (!text.isEmpty()) {
				text.append(" | causa: ");
			}
			text.append(current.getClass().getSimpleName()).append(": ").append(current.getMessage());
			if (current.getCause() == current) {
				break;
			}
		}
		return text.length() > REASON_MAX_LENGTH
				? text.substring(0, REASON_MAX_LENGTH) + "...(truncado)"
				: text.toString();
	}

	private String guessContentType(String filename) {
		int dot = filename.lastIndexOf('.');
		String extension = dot == -1 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
		return CONTENT_TYPES_BY_EXTENSION.getOrDefault(extension, "application/octet-stream");
	}

}
