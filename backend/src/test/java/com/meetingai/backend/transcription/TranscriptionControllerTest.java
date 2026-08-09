package com.meetingai.backend.transcription;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

@WebMvcTest(TranscriptionController.class)
@WithMockUser
class TranscriptionControllerTest {

	@Autowired
	private MockMvcTester mockMvc;

	@Test
	void getIsNotImplementedYet() {
		assertThat(mockMvc.get().uri("/api/meetings/{id}/transcription", UUID.randomUUID()))
				.hasStatus(HttpStatus.NOT_IMPLEMENTED);
	}

}
