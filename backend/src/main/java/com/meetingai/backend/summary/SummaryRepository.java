package com.meetingai.backend.summary;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SummaryRepository extends JpaRepository<Summary, UUID> {

	Optional<Summary> findByMeetingId(UUID meetingId);

}
