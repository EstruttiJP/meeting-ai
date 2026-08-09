package com.meetingai.backend.meeting;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

// OAuth2ClientWebSecurityAutoConfiguration precisa de um bean HttpSecurity que só existe no
// contexto web completo, não numa fatia @WebMvcTest — sem excluir, o slice falha ao subir.
@WebMvcTest(
		controllers = MeetingController.class,
		excludeAutoConfiguration = { OAuth2ClientAutoConfiguration.class, OAuth2ClientWebSecurityAutoConfiguration.class })
@WithMockUser
class MeetingControllerTest {

	@Autowired
	private MockMvcTester mockMvc;

	@Test
	void listIsNotImplementedYet() {
		assertThat(mockMvc.get().uri("/api/meetings")).hasStatus(HttpStatus.NOT_IMPLEMENTED);
	}

	@Test
	void getByIdIsNotImplementedYet() {
		assertThat(mockMvc.get().uri("/api/meetings/{id}", UUID.randomUUID()))
				.hasStatus(HttpStatus.NOT_IMPLEMENTED);
	}

	@Test
	void uploadWithValidRequestIsNotImplementedYet() {
		MockMultipartFile file = new MockMultipartFile("file", "reuniao.mp3", "audio/mpeg", "conteudo".getBytes());

		assertThat(mockMvc.perform(multipart("/api/meetings").file(file).param("title", "Reunião de vendas")))
				.hasStatus(HttpStatus.NOT_IMPLEMENTED);
	}

	@Test
	void uploadWithoutTitleIsRejected() {
		MockMultipartFile file = new MockMultipartFile("file", "reuniao.mp3", "audio/mpeg", "conteudo".getBytes());

		assertThat(mockMvc.perform(multipart("/api/meetings").file(file)))
				.hasStatus(HttpStatus.BAD_REQUEST);
	}

}
