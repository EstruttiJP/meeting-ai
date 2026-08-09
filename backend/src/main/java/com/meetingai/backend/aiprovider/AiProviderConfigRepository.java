package com.meetingai.backend.aiprovider;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AiProviderConfigRepository extends JpaRepository<AiProviderConfig, UUID> {

	List<AiProviderConfig> findByUserId(UUID userId);

	Optional<AiProviderConfig> findByUserIdAndProvider(UUID userId, AiProvider provider);

}
