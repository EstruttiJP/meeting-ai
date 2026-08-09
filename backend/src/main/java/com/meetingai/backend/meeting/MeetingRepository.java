package com.meetingai.backend.meeting;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingRepository extends JpaRepository<Meeting, UUID> {

	List<Meeting> findByUserId(UUID userId);

	List<Meeting> findByStatusNotAndExpiresAtBefore(MeetingStatus status, Instant instant);

}
