package com.meetingai.backend.health;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

// OAuth2ClientWebSecurityAutoConfiguration precisa de um bean HttpSecurity que só existe no
// contexto web completo, não numa fatia @WebMvcTest — sem excluir, o slice falha ao subir.
@WebMvcTest(
		controllers = HealthController.class,
		excludeAutoConfiguration = { OAuth2ClientAutoConfiguration.class, OAuth2ClientWebSecurityAutoConfiguration.class })
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
