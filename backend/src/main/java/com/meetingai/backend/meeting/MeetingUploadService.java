package com.meetingai.backend.meeting;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.meetingai.backend.storage.StorageException;
import com.meetingai.backend.storage.StorageService;
import com.meetingai.backend.usagequota.UsageQuotaService;
import com.meetingai.backend.user.User;

/**
 * Valida e recebe o upload de um arquivo de reunião. Não dispara o pipeline
 * assíncrono (transcrição/resumo) — isso é responsabilidade de quem chama
 * {@link #upload}, depois que o {@link Meeting} está persistido.
 */
@Service
public class MeetingUploadService {

	private static final Set<String> ALLOWED_EXTENSIONS = Set.of("mp3", "mp4", "wav", "m4a");

	private final MeetingRepository meetingRepository;
	private final UsageQuotaService usageQuotaService;
	private final StorageService storageService;
	private final long maxFileSizeBytes;

	public MeetingUploadService(MeetingRepository meetingRepository,
			UsageQuotaService usageQuotaService,
			StorageService storageService,
			@Value("${app.meeting.max-file-size-mb}") long maxFileSizeMb) {
		this.meetingRepository = meetingRepository;
		this.usageQuotaService = usageQuotaService;
		this.storageService = storageService;
		this.maxFileSizeBytes = maxFileSizeMb * 1024 * 1024;
	}

	@Transactional
	public Meeting upload(User user, String title, MultipartFile file) {
		String originalFilename = file.getOriginalFilename();
		validateFormat(originalFilename);
		validateSize(file.getSize());

		// Checa a cota antes de gravar qualquer coisa em disco.
		usageQuotaService.consumeUploadSlot(user);

		String storageKey = buildStorageKey(user, originalFilename);
		try (InputStream content = file.getInputStream()) {
			storageService.store(storageKey, content, file.getSize(), file.getContentType());
		} catch (IOException e) {
			throw new StorageException("Falha ao ler o arquivo enviado", e);
		}

		return meetingRepository.save(new Meeting(user, title, originalFilename, storageKey));
	}

	private void validateFormat(String originalFilename) {
		if (!ALLOWED_EXTENSIONS.contains(extensionOf(originalFilename))) {
			throw new UnsupportedMeetingFormatException(originalFilename);
		}
	}

	private void validateSize(long size) {
		if (size > maxFileSizeBytes) {
			throw new MeetingFileTooLargeException(maxFileSizeBytes);
		}
	}

	private String extensionOf(String filename) {
		if (filename == null) {
			return "";
		}
		int dot = filename.lastIndexOf('.');
		return dot == -1 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
	}

	private String buildStorageKey(User user, String originalFilename) {
		String sanitized = originalFilename == null ? "arquivo" : originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
		return "%s/%s-%s".formatted(user.getId(), UUID.randomUUID(), sanitized);
	}

}
