package com.meetingai.backend.crm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class PipedriveOAuthClientTest {

	private MockRestServiceServer mockServer;
	private PipedriveOAuthClient client;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		mockServer = MockRestServiceServer.bindTo(builder).build();
		client = new PipedriveOAuthClient(builder, "https://pipedrive.local/oauth/token",
				"client-id", "client-secret", "http://localhost:4200/settings");
	}

	@Test
	void exchangesAuthorizationCodeForTokens() {
		mockServer.expect(requestTo("https://pipedrive.local/oauth/token"))
				.andExpect(method(HttpMethod.POST))
				.andRespond(withSuccess("""
						{"access_token": "access-123", "refresh_token": "refresh-456", "expires_in": 3600}
						""", MediaType.APPLICATION_JSON));

		PipedriveTokens tokens = client.exchangeAuthorizationCode("auth-code-abc");

		assertThat(tokens.accessToken()).isEqualTo("access-123");
		assertThat(tokens.refreshToken()).isEqualTo("refresh-456");
		assertThat(tokens.expiresAt()).isNotNull();
	}

	@Test
	void throwsWhenPipedriveRejectsTheCode() {
		mockServer.expect(requestTo("https://pipedrive.local/oauth/token"))
				.andRespond(withServerError());

		assertThatThrownBy(() -> client.exchangeAuthorizationCode("invalid-code"))
				.isInstanceOf(CrmConnectionException.class);
	}

}
