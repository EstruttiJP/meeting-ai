package com.meetingai.backend.transcription;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.meetingai.backend.security.CurrentUserService;
import com.meetingai.backend.user.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

// OAuth2ClientWebSecurityAutoConfiguration precisa de um bean HttpSecurity que só existe no
// contexto web completo, não numa fatia @WebMvcTest — sem excluir, o slice falha ao subir.
@WebMvcTest(
		controllers = TranscriptionController.class,
		excludeAutoConfiguration = { OAuth2ClientAutoConfiguration.class, OAuth2ClientWebSecurityAutoConfiguration.class })
@WithMockUser
class TranscriptionControllerTest {

	@Autowired
	private MockMvcTester mockMvc;

	@MockitoBean
	private CurrentUserService currentUserService;

	@MockitoBean
	private TranscriptionService transcriptionService;

	private final User user = new User("google-sub-1", "dev@meetingai.com", "Dev User", null);

	@Test
	void getReturnsTranscriptionForOwnedMeeting() {
		UUID meetingId = UUID.randomUUID();
		TranscriptionResponse response = new TranscriptionResponse(
				UUID.randomUUID(), meetingId, "conteúdo transcrito", "pt", "whisper-local", java.time.Instant.now());
		given(currentUserService.getCurrentUser()).willReturn(user);
		given(transcriptionService.getForMeeting(user, meetingId)).willReturn(response);

		assertThat(mockMvc.get().uri("/api/meetings/{id}/transcription", meetingId))
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.content")
				.isEqualTo("conteúdo transcrito");
	}

	@Test
	void getOfMeetingWithoutTranscriptionYetReturnsNotFound() {
		UUID meetingId = UUID.randomUUID();
		given(currentUserService.getCurrentUser()).willReturn(user);
		given(transcriptionService.getForMeeting(user, meetingId))
				.willThrow(new TranscriptionNotFoundException(meetingId));

		assertThat(mockMvc.get().uri("/api/meetings/{id}/transcription", meetingId))
				.hasStatus(HttpStatus.NOT_FOUND);
	}

}
