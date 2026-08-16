package com.meetingai.backend.meeting;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.meetingai.backend.storage.StorageException;
import com.meetingai.backend.storage.StorageService;
import com.meetingai.backend.user.User;

/**
 * Entrega o áudio original de uma reunião para o player da tela de revisão.
 * O arquivo é servido pela própria API, nunca por URL direta de storage: assim
 * a checagem de dono e de expiração acontece a cada request, e a chave de
 * armazenamento não vaza para o navegador.
 */
@Service
public class MeetingAudioService {

	private static final Map<String, String> CONTENT_TYPES_BY_EXTENSION = Map.of(
			"mp3", "audio/mpeg",
			"mp4", "video/mp4",
			"wav", "audio/wav",
			"m4a", "audio/mp4");

	private final MeetingAccessService meetingAccessService;
	private final StorageService storageService;

	public MeetingAudioService(MeetingAccessService meetingAccessService, StorageService storageService) {
		this.meetingAccessService = meetingAccessService;
		this.storageService = storageService;
	}

	public MeetingAudio load(User user, UUID meetingId) {
		Meeting meeting = meetingAccessService.getOwnedMeeting(user, meetingId);

		if (meeting.getStatus() == MeetingStatus.EXPIRED) {
			throw new MeetingAudioUnavailableException(
					"O áudio desta reunião foi removido após o período de retenção.");
		}
		// Também barra a janela entre o expires_at vencer e o job de expiração
		// rodar: nessa fresta o status ainda não mudou, mas o arquivo já não
		// deveria estar acessível.
		if (meeting.getExpiresAt() != null && meeting.getExpiresAt().isBefore(Instant.now())) {
			throw new MeetingAudioUnavailableException(
					"O áudio desta reunião foi removido após o período de retenção.");
		}

		Resource resource;
		try {
			resource = new InputStreamResource(storageService.retrieve(meeting.getStorageKey()));
		} catch (StorageException e) {
			// O arquivo sumiu do storage sem a reunião ter sido marcada como
			// expirada. Para quem está na tela o efeito é o mesmo, então vale a
			// mesma mensagem — a causa técnica fica no log do storage.
			throw new MeetingAudioUnavailableException(
					"O áudio original desta reunião não está mais disponível.");
		}
		return new MeetingAudio(resource, contentType(meeting.getOriginalFilename()), meeting.getOriginalFilename());
	}

	private static String contentType(String filename) {
		int dot = filename.lastIndexOf('.');
		String extension = dot == -1 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
		return CONTENT_TYPES_BY_EXTENSION.getOrDefault(extension, "application/octet-stream");
	}

	public record MeetingAudio(Resource resource, String contentType, String filename) {
	}

}
