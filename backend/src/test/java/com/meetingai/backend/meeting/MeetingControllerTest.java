package com.meetingai.backend.meeting;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.meetingai.backend.security.CurrentUserService;
import com.meetingai.backend.usagequota.UsageQuotaExceededException;
import com.meetingai.backend.user.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

// OAuth2ClientWebSecurityAutoConfiguration precisa de um bean HttpSecurity que só existe no
// contexto web completo, não numa fatia @WebMvcTest — sem excluir, o slice falha ao subir.
@WebMvcTest(
		controllers = MeetingController.class,
		excludeAutoConfiguration = { OAuth2ClientAutoConfiguration.class, OAuth2ClientWebSecurityAutoConfiguration.class })
@WithMockUser
class MeetingControllerTest {

	@Autowired
	private MockMvcTester mockMvc;

	@MockitoBean
	private CurrentUserService currentUserService;

	@MockitoBean
	private MeetingUploadService meetingUploadService;

	@MockitoBean
	private MeetingPipelineService meetingPipelineService;

	@MockitoBean
	private MeetingAccessService meetingAccessService;

	@MockitoBean
	private MeetingAudioService meetingAudioService;

	private final User user = new User("google-sub-1", "dev@meetingai.com", "Dev User", null);

	@Test
	void listReturnsUsersMeetings() {
		Meeting meeting = new Meeting(user, "Reunião de vendas", "reuniao.mp3", "user-1/key.mp3");
		given(currentUserService.getCurrentUser()).willReturn(user);
		given(meetingAccessService.listForUser(user)).willReturn(java.util.List.of(meeting));

		assertThat(mockMvc.get().uri("/api/meetings"))
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$[0].title")
				.isEqualTo("Reunião de vendas");
	}

	@Test
	void getByIdReturnsOwnedMeeting() {
		UUID id = UUID.randomUUID();
		Meeting meeting = new Meeting(user, "Reunião de vendas", "reuniao.mp3", "user-1/key.mp3");
		given(currentUserService.getCurrentUser()).willReturn(user);
		given(meetingAccessService.getOwnedMeeting(user, id)).willReturn(meeting);

		assertThat(mockMvc.get().uri("/api/meetings/{id}", id))
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.status")
				.isEqualTo("UPLOADED");
	}

	@Test
	void getByIdOfMeetingNotOwnedReturnsNotFound() {
		UUID id = UUID.randomUUID();
		given(currentUserService.getCurrentUser()).willReturn(user);
		given(meetingAccessService.getOwnedMeeting(user, id)).willThrow(new MeetingNotFoundException(id));

		assertThat(mockMvc.get().uri("/api/meetings/{id}", id))
				.hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void uploadWithValidRequestReturnsCreatedMeeting() {
		MockMultipartFile file = new MockMultipartFile("file", "reuniao.mp3", "audio/mpeg", "conteudo".getBytes());
		Meeting meeting = new Meeting(user, "Reunião de vendas", "reuniao.mp3", "user-1/key-reuniao.mp3");
		given(currentUserService.getCurrentUser()).willReturn(user);
		given(meetingUploadService.upload(any(User.class), anyString(), any())).willReturn(meeting);

		assertThat(mockMvc.perform(multipart("/api/meetings").file(file).param("title", "Reunião de vendas")))
				.hasStatus(HttpStatus.CREATED)
				.bodyJson()
				.extractingPath("$.status")
				.isEqualTo("UPLOADED");
		verify(meetingPipelineService).process(meeting.getId());
	}

	@Test
	void uploadWithoutTitleIsRejected() {
		MockMultipartFile file = new MockMultipartFile("file", "reuniao.mp3", "audio/mpeg", "conteudo".getBytes());

		assertThat(mockMvc.perform(multipart("/api/meetings").file(file)))
				.hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	void uploadOverQuotaIsRejectedWithForbidden() {
		MockMultipartFile file = new MockMultipartFile("file", "reuniao.mp3", "audio/mpeg", "conteudo".getBytes());
		given(currentUserService.getCurrentUser()).willReturn(user);
		given(meetingUploadService.upload(any(User.class), anyString(), any()))
				.willThrow(new UsageQuotaExceededException(10));

		assertThat(mockMvc.perform(multipart("/api/meetings").file(file).param("title", "Reunião de vendas")))
				.hasStatus(HttpStatus.FORBIDDEN);
	}

	@Test
	void uploadWithUnsupportedFormatIsRejected() {
		MockMultipartFile file = new MockMultipartFile("file", "reuniao.txt", "text/plain", "conteudo".getBytes());
		given(currentUserService.getCurrentUser()).willReturn(user);
		given(meetingUploadService.upload(any(User.class), anyString(), any()))
				.willThrow(new UnsupportedMeetingFormatException("reuniao.txt"));

		assertThat(mockMvc.perform(multipart("/api/meetings").file(file).param("title", "Reunião de vendas")))
				.hasStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
	}

}
