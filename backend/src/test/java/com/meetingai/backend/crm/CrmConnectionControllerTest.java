package com.meetingai.backend.crm;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

@WebMvcTest(CrmConnectionController.class)
@WithMockUser
class CrmConnectionControllerTest {

	@Autowired
	private MockMvcTester mockMvc;

	@Test
	void meIsNotImplementedYet() {
		assertThat(mockMvc.get().uri("/api/crm-connections/me")).hasStatus(HttpStatus.NOT_IMPLEMENTED);
	}

	@Test
	void connectWithValidBodyIsNotImplementedYet() {
		assertThat(mockMvc.post().uri("/api/crm-connections")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"provider": "PIPEDRIVE", "authorizationCode": "abc123"}
						"""))
				.hasStatus(HttpStatus.NOT_IMPLEMENTED);
	}

	@Test
	void connectWithMissingCodeIsRejected() {
		assertThat(mockMvc.post().uri("/api/crm-connections")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"provider": "PIPEDRIVE", "authorizationCode": ""}
						"""))
				.hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	void disconnectIsNotImplementedYet() {
		assertThat(mockMvc.delete().uri("/api/crm-connections/me")).hasStatus(HttpStatus.NOT_IMPLEMENTED);
	}

}
