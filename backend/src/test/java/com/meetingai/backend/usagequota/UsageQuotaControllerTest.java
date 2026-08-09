package com.meetingai.backend.usagequota;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

@WebMvcTest(UsageQuotaController.class)
@WithMockUser
class UsageQuotaControllerTest {

	@Autowired
	private MockMvcTester mockMvc;

	@Test
	void meIsNotImplementedYet() {
		assertThat(mockMvc.get().uri("/api/usage-quota/me")).hasStatus(HttpStatus.NOT_IMPLEMENTED);
	}

}
