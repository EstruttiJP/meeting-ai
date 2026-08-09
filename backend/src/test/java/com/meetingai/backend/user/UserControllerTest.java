package com.meetingai.backend.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.meetingai.backend.security.CurrentUserService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

// OAuth2ClientWebSecurityAutoConfiguration precisa de um bean HttpSecurity que só existe no
// contexto web completo, não numa fatia @WebMvcTest — sem excluir, o slice falha ao subir.
@WebMvcTest(
		controllers = UserController.class,
		excludeAutoConfiguration = { OAuth2ClientAutoConfiguration.class, OAuth2ClientWebSecurityAutoConfiguration.class })
@WithMockUser
class UserControllerTest {

	@Autowired
	private MockMvcTester mockMvc;

	@MockitoBean
	private CurrentUserService currentUserService;

	@Test
	void meReturnsAuthenticatedUser() {
		User user = new User("google-sub-123", "dev@meetingai.com", "Dev User", "https://example.com/pic.png");
		given(currentUserService.getCurrentUser()).willReturn(user);

		assertThat(mockMvc.get().uri("/api/users/me"))
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.email")
				.isEqualTo("dev@meetingai.com");
	}

}
