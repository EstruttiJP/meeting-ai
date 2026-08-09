package com.meetingai.backend.crm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Cria negócios e anexa notas no Pipedrive via API v1, usando o access token do usuário. */
@Service
class PipedriveClient {

	private final RestClient restClient;

	PipedriveClient(RestClient.Builder restClientBuilder, @Value("${app.crm.pipedrive.api-base-url}") String baseUrl) {
		this.restClient = restClientBuilder.baseUrl(baseUrl).build();
	}

	long createDeal(String accessToken, String title) {
		DealResponse response;
		try {
			response = restClient.post()
					.uri("/v1/deals")
					.headers(headers -> headers.setBearerAuth(accessToken))
					.contentType(MediaType.APPLICATION_JSON)
					.body(new DealRequest(title))
					.retrieve()
					.body(DealResponse.class);
		} catch (RestClientException e) {
			throw new CrmConnectionException("Falha ao criar negócio no Pipedrive", e);
		}
		if (response == null || !response.success() || response.data() == null) {
			throw new CrmConnectionException("Pipedrive não confirmou a criação do negócio");
		}
		return response.data().id();
	}

	void addNote(String accessToken, long dealId, String content) {
		try {
			restClient.post()
					.uri("/v1/notes")
					.headers(headers -> headers.setBearerAuth(accessToken))
					.contentType(MediaType.APPLICATION_JSON)
					.body(new NoteRequest(dealId, content))
					.retrieve()
					.toBodilessEntity();
		} catch (RestClientException e) {
			throw new CrmConnectionException("Falha ao anexar o resumo ao negócio no Pipedrive", e);
		}
	}

	private record DealRequest(String title) {
	}

	private record DealData(long id) {
	}

	private record DealResponse(boolean success, DealData data) {
	}

	private record NoteRequest(@JsonProperty("deal_id") long dealId, String content) {
	}

}
