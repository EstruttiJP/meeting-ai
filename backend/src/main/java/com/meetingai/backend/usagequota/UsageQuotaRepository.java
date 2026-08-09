package com.meetingai.backend.usagequota;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UsageQuotaRepository extends JpaRepository<UsageQuota, UUID> {

	Optional<UsageQuota> findByUserIdAndMonthReference(UUID userId, String monthReference);

}
