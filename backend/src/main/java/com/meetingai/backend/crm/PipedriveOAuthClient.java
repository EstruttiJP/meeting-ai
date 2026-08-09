package com.meetingai.backend.crm;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Troca o código de autorização do fluxo OAuth do Pipedrive por um par de tokens. */
@Service
class PipedriveOAuthClient {

	private final RestClient restClient;
	private final String clientId;
	private final String clientSecret;
	private final String redirectUri;

	PipedriveOAuthClient(RestClient.Builder restClientBuilder,
			@Value("${app.crm.pipedrive.token-url}") String tokenUrl,
			@Value("${app.crm.pipedrive.client-id}") String clientId,
			@Value("${app.crm.pipedrive.client-secret}") String clientSecret,
			@Value("${app.crm.pipedrive.redirect-uri}") String redirectUri) {
		this.restClient = restClientBuilder.baseUrl(tokenUrl).build();
		this.clientId = clientId;
		this.clientSecret = clientSecret;
		this.redirectUri = redirectUri;
	}

	PipedriveTokens exchangeAuthorizationCode(String authorizationCode) {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("grant_type", "authorization_code");
		form.add("code", authorizationCode);
		form.add("redirect_uri", redirectUri);

		TokenResponse response;
		try {
			response = restClient.post()
					.contentType(MediaType.APPLICATION_FORM_URLENCODED)
					.headers(headers -> headers.setBasicAuth(clientId, clientSecret))
					.body(form)
					.retrieve()
					.body(TokenResponse.class);
		} catch (RestClientException e) {
			throw new CrmConnectionException("Falha ao trocar o código OAuth do Pipedrive por um token", e);
		}
		if (response == null || response.accessToken() == null) {
			throw new CrmConnectionException("Resposta inválida do Pipedrive ao trocar o código OAuth");
		}
		Instant expiresAt = response.expiresIn() == null ? null : Instant.now().plusSeconds(response.expiresIn());
		return new PipedriveTokens(response.accessToken(), response.refreshToken(), expiresAt);
	}

	private record TokenResponse(
			@JsonProperty("access_token") String accessToken,
			@JsonProperty("refresh_token") String refreshToken,
			@JsonProperty("expires_in") Long expiresIn) {
	}

}
