package com.meetingai.backend.aiprovider;

import java.util.List;
import java.util.UUID;

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
		controllers = AiProviderConfigController.class,
		excludeAutoConfiguration = { OAuth2ClientAutoConfiguration.class, OAuth2ClientWebSecurityAutoConfiguration.class })
@WithMockUser
class AiProviderConfigControllerTest {

	@Autowired
	private MockMvcTester mockMvc;

	@MockitoBean
	private CurrentUserService currentUserService;

	@MockitoBean
	private AiProviderConfigService aiProviderConfigService;

	private final User user = new User("google-sub-1", "dev@meetingai.com", "Dev User", null);

	@Test
	void listReturnsUsersConfigs() {
		AiProviderConfig config = new AiProviderConfig(user, AiProvider.OPENROUTER, null, true);
		given(currentUserService.getCurrentUser()).willReturn(user);
		given(aiProviderConfigService.list(user)).willReturn(List.of(config));

		assertThat(mockMvc.get().uri("/api/ai-provider-configs"))
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$[0].provider")
				.isEqualTo("OPENROUTER");
	}

	@Test
	void createWithValidBodyReturnsCreatedConfigWithoutLeakingKey() {
		AiProviderConfig saved = new AiProviderConfig(user, AiProvider.GEMINI, "encrypted-value", true);
		given(currentUserService.getCurrentUser()).willReturn(user);
		given(aiProviderConfigService.save(any(User.class), any(AiProviderConfigRequest.class))).willReturn(saved);

		assertThat(mockMvc.post().uri("/api/ai-provider-configs")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"provider": "GEMINI", "apiKey": "sk-super-secret", "isDefault": true}
						"""))
				.hasStatus(HttpStatus.CREATED)
				.bodyJson()
				.extractingPath("$.hasApiKey")
				.isEqualTo(true);
	}

	@Test
	void createResponseNeverContainsRawApiKey() {
		AiProviderConfig saved = new AiProviderConfig(user, AiProvider.GEMINI, "encrypted-value", true);
		given(currentUserService.getCurrentUser()).willReturn(user);
		given(aiProviderConfigService.save(any(User.class), any(AiProviderConfigRequest.class))).willReturn(saved);

		assertThat(mockMvc.post().uri("/api/ai-provider-configs")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"provider": "GEMINI", "apiKey": "sk-super-secret", "isDefault": true}
						"""))
				.bodyText()
				.doesNotContain("sk-super-secret");
	}

	@Test
	void createWithMissingProviderIsRejectedWithoutEchoingRejectedValue() {
		given(currentUserService.getCurrentUser()).willReturn(user);

		assertThat(mockMvc.post().uri("/api/ai-provider-configs")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"apiKey": "sk-super-secret", "isDefault": true}
						"""))
				.hasStatus(HttpStatus.BAD_REQUEST)
				.bodyText()
				.doesNotContain("sk-super-secret");
	}

	@Test
	void deleteRemovesConfig() {
		given(currentUserService.getCurrentUser()).willReturn(user);

		assertThat(mockMvc.delete().uri("/api/ai-provider-configs/{id}", UUID.randomUUID()))
				.hasStatus(HttpStatus.NO_CONTENT);
	}

	@Test
	void deleteOfConfigBelongingToAnotherUserReturnsNotFound() {
		UUID id = UUID.randomUUID();
		given(currentUserService.getCurrentUser()).willReturn(user);
		willThrow(new AiProviderConfigNotFoundException(id)).given(aiProviderConfigService).delete(user, id);

		assertThat(mockMvc.delete().uri("/api/ai-provider-configs/{id}", id))
				.hasStatus(HttpStatus.NOT_FOUND);
	}

}
