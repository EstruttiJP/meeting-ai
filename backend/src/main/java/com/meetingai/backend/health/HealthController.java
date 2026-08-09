package com.meetingai.backend.health;

import java.time.Instant;
import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = {"http://localhost:4200"})
public class HealthController {

	@GetMapping("/api/health")
	public Map<String, Object> health() {
		return Map.of(
				"status", "UP",
				"service", "meeting-ai-backend",
				"timestamp", Instant.now().toString());
	}

}
