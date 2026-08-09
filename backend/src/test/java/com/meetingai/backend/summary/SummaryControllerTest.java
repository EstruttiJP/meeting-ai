package com.meetingai.backend.summary;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

// OAuth2ClientWebSecurityAutoConfiguration precisa de um bean HttpSecurity que só existe no
// contexto web completo, não numa fatia @WebMvcTest — sem excluir, o slice falha ao subir.
@WebMvcTest(
		controllers = SummaryController.class,
		excludeAutoConfiguration = { OAuth2ClientAutoConfiguration.class, OAuth2ClientWebSecurityAutoConfiguration.class })
@WithMockUser
class SummaryControllerTest {

	@Autowired
	private MockMvcTester mockMvc;

	@Test
	void getIsNotImplementedYet() {
		assertThat(mockMvc.get().uri("/api/meetings/{id}/summary", UUID.randomUUID()))
				.hasStatus(HttpStatus.NOT_IMPLEMENTED);
	}

	@Test
	void updateWithValidBodyIsNotImplementedYet() {
		assertThat(mockMvc.put().uri("/api/meetings/{id}/summary", UUID.randomUUID())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"content": {"summary": "Cliente interessado"}}
						"""))
				.hasStatus(HttpStatus.NOT_IMPLEMENTED);
	}

	@Test
	void updateWithBlankSummaryIsRejected() {
		assertThat(mockMvc.put().uri("/api/meetings/{id}/summary", UUID.randomUUID())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"content": {"summary": ""}}
						"""))
				.hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	void sendToCrmIsNotImplementedYet() {
		assertThat(mockMvc.post().uri("/api/meetings/{id}/summary/send-to-crm", UUID.randomUUID()))
				.hasStatus(HttpStatus.NOT_IMPLEMENTED);
	}

}
