package com.meetingai.backend.meeting;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetingai.backend.storage.StorageException;
import com.meetingai.backend.storage.StorageService;

/**
 * Apaga o arquivo bruto de reuniões cujo expires_at já passou e marca a
 * reunião como EXPIRED — o resumo em texto continua no banco (pesa muito
 * menos que o áudio, não precisa expirar junto).
 */
@Service
public class MeetingExpirationJob {

	private static final Logger log = LoggerFactory.getLogger(MeetingExpirationJob.class);

	private final MeetingRepository meetingRepository;
	private final StorageService storageService;

	public MeetingExpirationJob(MeetingRepository meetingRepository, StorageService storageService) {
		this.meetingRepository = meetingRepository;
		this.storageService = storageService;
	}

	@Scheduled(cron = "${app.meeting.expiration.cron}")
	@Transactional
	public void expireOldMeetings() {
		List<Meeting> expired = meetingRepository.findByStatusNotAndExpiresAtBefore(MeetingStatus.EXPIRED, Instant.now());
		for (Meeting meeting : expired) {
			try {
				storageService.delete(meeting.getStorageKey());
			} catch (StorageException e) {
				log.warn("Falha ao apagar arquivo da reunião {} durante expiração", meeting.getId(), e);
			}
			meeting.markExpired();
			meetingRepository.save(meeting);
		}
	}

}
