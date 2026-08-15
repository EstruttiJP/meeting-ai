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

	// Regressão: CookieCsrfTokenRepository só escreve o cookie XSRF-TOKEN se algo
	// resolver o CsrfToken durante a requisição. Sem o CsrfCookieFilter forçando essa
	// resolução, o cookie nunca aparecia e todo POST/PUT/DELETE do frontend (que
	// depende dele pro header X-XSRF-TOKEN) caía num 403 antes de chegar no controller.
	@Test
	void csrfCookieIsSetOnFirstRequest() {
		assertThat(mockMvc.get().uri("/api/users/me"))
				.cookies().containsKey("XSRF-TOKEN");
	}

}
