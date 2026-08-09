package com.meetingai.backend.crm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class PipedriveClientTest {

	private MockRestServiceServer mockServer;
	private PipedriveClient client;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		mockServer = MockRestServiceServer.bindTo(builder).build();
		client = new PipedriveClient(builder, "https://pipedrive.local");
	}

	@Test
	void createDealReturnsIdAndSendsBearerToken() {
		mockServer.expect(requestTo("https://pipedrive.local/v1/deals"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header("Authorization", "Bearer access-123"))
				.andExpect(content().string(containsString("Reunião de vendas")))
				.andRespond(withSuccess("""
						{"success": true, "data": {"id": 42}}
						""", MediaType.APPLICATION_JSON));

		long dealId = client.createDeal("access-123", "Reunião de vendas");

		assertThat(dealId).isEqualTo(42L);
	}

	@Test
	void createDealThrowsWhenPipedriveReportsFailure() {
		mockServer.expect(requestTo("https://pipedrive.local/v1/deals"))
				.andRespond(withSuccess("""
						{"success": false}
						""", MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> client.createDeal("access-123", "Reunião de vendas"))
				.isInstanceOf(CrmConnectionException.class);
	}

	@Test
	void createDealThrowsOnHttpFailure() {
		mockServer.expect(requestTo("https://pipedrive.local/v1/deals"))
				.andRespond(withServerError());

		assertThatThrownBy(() -> client.createDeal("access-123", "Reunião de vendas"))
				.isInstanceOf(CrmConnectionException.class);
	}

	@Test
	void addNoteSendsDealIdAndContent() {
		mockServer.expect(requestTo("https://pipedrive.local/v1/notes"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(content().string(containsString("resumo da reunião")))
				.andRespond(withSuccess("{\"success\": true}", MediaType.APPLICATION_JSON));

		client.addNote("access-123", 42L, "resumo da reunião");
	}

}
