package com.meetingai.backend.summary;

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
		controllers = SummaryController.class,
		excludeAutoConfiguration = { OAuth2ClientAutoConfiguration.class, OAuth2ClientWebSecurityAutoConfiguration.class })
@WithMockUser
class SummaryControllerTest {

	@Autowired
	private MockMvcTester mockMvc;

	@MockitoBean
	private CurrentUserService currentUserService;

	@MockitoBean
	private SummaryService summaryService;

	private final User user = new User("google-sub-1", "dev@meetingai.com", "Dev User", null);

	@Test
	void getReturnsSummaryForOwnedMeeting() {
		UUID meetingId = UUID.randomUUID();
		SummaryContent content = new SummaryContent("Cliente interessado", List.of(), List.of(), List.of(), null, List.of());
		SummaryResponse response = new SummaryResponse(UUID.randomUUID(), meetingId, content, false, null);
		given(currentUserService.getCurrentUser()).willReturn(user);
		given(summaryService.getForMeeting(user, meetingId)).willReturn(response);

		assertThat(mockMvc.get().uri("/api/meetings/{id}/summary", meetingId))
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.content.summary")
				.isEqualTo("Cliente interessado");
	}

	@Test
	void getOfMeetingNotOwnedByUserReturnsNotFound() {
		UUID meetingId = UUID.randomUUID();
		given(currentUserService.getCurrentUser()).willReturn(user);
		given(summaryService.getForMeeting(user, meetingId)).willThrow(new com.meetingai.backend.meeting.MeetingNotFoundException(meetingId));

		assertThat(mockMvc.get().uri("/api/meetings/{id}/summary", meetingId))
				.hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void updateWithValidBodyApprovesAndReturnsUpdatedSummary() {
		UUID meetingId = UUID.randomUUID();
		SummaryContent editedContent = new SummaryContent("Cliente muito interessado", List.of(), List.of(), List.of(), null, List.of());
		SummaryResponse response = new SummaryResponse(UUID.randomUUID(), meetingId, editedContent, true, java.time.Instant.now());
		given(currentUserService.getCurrentUser()).willReturn(user);
		given(summaryService.approve(any(User.class), org.mockito.ArgumentMatchers.eq(meetingId), any(SummaryContent.class)))
				.willReturn(response);

		assertThat(mockMvc.put().uri("/api/meetings/{id}/summary", meetingId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"content": {"summary": "Cliente muito interessado"}}
						"""))
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.approved")
				.isEqualTo(true);
	}

	@Test
	void updateWithBlankSummaryIsRejected() {
		assertThat(mockMvc.put().uri("/api/meetings/{id}/summary", UUID.randomUUID())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"content": {"summary": ""}}
						"""))
				.hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	void updateOfMeetingWithoutSummaryYetReturnsNotFound() {
		UUID meetingId = UUID.randomUUID();
		given(currentUserService.getCurrentUser()).willReturn(user);
		willThrow(new SummaryNotFoundException(meetingId))
				.given(summaryService).approve(any(User.class), org.mockito.ArgumentMatchers.eq(meetingId), any(SummaryContent.class));

		assertThat(mockMvc.put().uri("/api/meetings/{id}/summary", meetingId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"content": {"summary": "Cliente interessado"}}
						"""))
				.hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void sendToCrmIsNotImplementedYet() {
		assertThat(mockMvc.post().uri("/api/meetings/{id}/summary/send-to-crm", UUID.randomUUID()))
				.hasStatus(HttpStatus.NOT_IMPLEMENTED);
	}

}
