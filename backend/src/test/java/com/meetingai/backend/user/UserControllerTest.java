package com.meetingai.backend.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

@WebMvcTest(UserController.class)
@WithMockUser
class UserControllerTest {

	@Autowired
	private MockMvcTester mockMvc;

	@Test
	void meIsNotImplementedYet() {
		assertThat(mockMvc.get().uri("/api/users/me"))
				.hasStatus(org.springframework.http.HttpStatus.NOT_IMPLEMENTED);
	}

}
