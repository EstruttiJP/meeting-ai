package com.meetingai.backend.crm;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CrmConnectionRepository extends JpaRepository<CrmConnection, UUID> {

	Optional<CrmConnection> findByUserId(UUID userId);

}
