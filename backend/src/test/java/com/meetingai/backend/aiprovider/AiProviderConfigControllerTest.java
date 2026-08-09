package com.meetingai.backend.aiprovider;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

@WebMvcTest(AiProviderConfigController.class)
@WithMockUser
class AiProviderConfigControllerTest {

	@Autowired
	private MockMvcTester mockMvc;

	@Test
	void listIsNotImplementedYet() {
		assertThat(mockMvc.get().uri("/api/ai-provider-configs")).hasStatus(HttpStatus.NOT_IMPLEMENTED);
	}

	@Test
	void createWithValidBodyIsNotImplementedYet() {
		assertThat(mockMvc.post().uri("/api/ai-provider-configs")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"provider": "OPENROUTER", "isDefault": true}
						"""))
				.hasStatus(HttpStatus.NOT_IMPLEMENTED);
	}

	@Test
	void createWithMissingProviderIsRejected() {
		assertThat(mockMvc.post().uri("/api/ai-provider-configs")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"isDefault": true}
						"""))
				.hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	void deleteIsNotImplementedYet() {
		assertThat(mockMvc.delete().uri("/api/ai-provider-configs/{id}", UUID.randomUUID()))
				.hasStatus(HttpStatus.NOT_IMPLEMENTED);
	}

}
