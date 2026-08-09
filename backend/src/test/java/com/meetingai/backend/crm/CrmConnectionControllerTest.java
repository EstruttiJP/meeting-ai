package com.meetingai.backend.crm;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.meetingai.backend.security.CurrentUserService;
import com.meetingai.backend.user.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

// OAuth2ClientWebSecurityAutoConfiguration precisa de um bean HttpSecurity que só existe no
// contexto web completo, não numa fatia @WebMvcTest — sem excluir, o slice falha ao subir.
@WebMvcTest(
		controllers = CrmConnectionController.class,
		excludeAutoConfiguration = { OAuth2ClientAutoConfiguration.class, OAuth2ClientWebSecurityAutoConfiguration.class })
@WithMockUser
class CrmConnectionControllerTest {

	@Autowired
	private MockMvcTester mockMvc;

	@MockitoBean
	private CurrentUserService currentUserService;

	@MockitoBean
	private CrmConnectionService crmConnectionService;

	private final User user = new User("google-sub-1", "dev@meetingai.com", "Dev User", null);

	@Test
	void meReturnsNotFoundWhenNoConnectionExists() {
		given(currentUserService.getCurrentUser()).willReturn(user);
		given(crmConnectionService.findForUser(user)).willReturn(Optional.empty());

		assertThat(mockMvc.get().uri("/api/crm-connections/me")).hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void meReturnsConnectionWhenPresent() {
		CrmConnection connection = new CrmConnection(user, CrmProvider.PIPEDRIVE, "encrypted-access", null, null);
		given(currentUserService.getCurrentUser()).willReturn(user);
		given(crmConnectionService.findForUser(user)).willReturn(Optional.of(connection));

		assertThat(mockMvc.get().uri("/api/crm-connections/me"))
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.provider")
				.isEqualTo("PIPEDRIVE");
	}

	@Test
	void connectWithValidBodyReturnsCreatedConnection() {
		CrmConnection connection = new CrmConnection(user, CrmProvider.PIPEDRIVE, "encrypted-access", "encrypted-refresh", null);
		given(currentUserService.getCurrentUser()).willReturn(user);
		given(crmConnectionService.connect(any(User.class), any(CrmConnectionRequest.class))).willReturn(connection);

		assertThat(mockMvc.post().uri("/api/crm-connections")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"provider": "PIPEDRIVE", "authorizationCode": "abc123"}
						"""))
				.hasStatus(HttpStatus.CREATED)
				.bodyJson()
				.extractingPath("$.provider")
				.isEqualTo("PIPEDRIVE");
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
	void connectWithInvalidCodeReturnsBadRequest() {
		given(currentUserService.getCurrentUser()).willReturn(user);
		willThrow(new CrmConnectionException("Falha ao trocar o código OAuth do Pipedrive por um token"))
				.given(crmConnectionService).connect(any(User.class), any(CrmConnectionRequest.class));

		assertThat(mockMvc.post().uri("/api/crm-connections")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"provider": "PIPEDRIVE", "authorizationCode": "invalid"}
						"""))
				.hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	void disconnectRemovesConnection() {
		given(currentUserService.getCurrentUser()).willReturn(user);

		assertThat(mockMvc.delete().uri("/api/crm-connections/me")).hasStatus(HttpStatus.NO_CONTENT);
	}

}
