package com.meetingai.backend.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

	@Autowired
	private MockMvcTester mockMvc;

	@Test
	void apiRoutesRequireAuthentication() {
		assertThat(mockMvc.get().uri("/api/users/me"))
				.hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void healthEndpointIsPublic() {
		assertThat(mockMvc.get().uri("/actuator/health"))
				.hasStatusOk();
	}

}
