package com.meetingai.backend.health;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

@WebMvcTest(HealthController.class)
class HealthControllerTest {

	@Autowired
	private MockMvcTester mockMvc;

	@Test
	void healthEndpointReportsUp() {
		assertThat(mockMvc.get().uri("/api/health"))
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.status")
				.isEqualTo("UP");
	}

}
