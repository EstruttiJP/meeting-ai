package com.meetingai.backend.transcription;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TranscriptionRepository extends JpaRepository<Transcription, UUID> {

	Optional<Transcription> findByMeetingId(UUID meetingId);

}
