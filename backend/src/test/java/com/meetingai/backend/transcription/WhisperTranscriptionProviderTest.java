package com.meetingai.backend.transcription;

import java.io.ByteArrayInputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class WhisperTranscriptionProviderTest {

	private MockRestServiceServer mockServer;
	private WhisperTranscriptionProvider provider;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		mockServer = MockRestServiceServer.bindTo(builder).build();
		provider = new WhisperTranscriptionProvider(builder, "http://whisper.local:9000");
	}

	@Test
	void reportsWhisperLocalAsProviderName() {
		assertThat(provider.providerName()).isEqualTo("whisper-local");
	}

	@Test
	void transcribesAudioAndParsesWhisperResponse() {
		mockServer.expect(requestTo(containsString("/asr")))
				.andExpect(method(HttpMethod.POST))
				.andRespond(withSuccess("""
						{"text": "Reunião sobre vendas do trimestre", "language": "pt"}
						""", MediaType.APPLICATION_JSON));

		TranscriptionResult result = provider.transcribe(
				new ByteArrayInputStream("audio".getBytes()), "reuniao.mp3", "audio/mpeg");

		assertThat(result.content()).isEqualTo("Reunião sobre vendas do trimestre");
		assertThat(result.language()).isEqualTo("pt");
	}

	@Test
	void throwsTranscriptionExceptionWhenResponseHasNoText() {
		mockServer.expect(requestTo(containsString("/asr")))
				.andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> provider.transcribe(
				new ByteArrayInputStream("audio".getBytes()), "reuniao.mp3", "audio/mpeg"))
				.isInstanceOf(TranscriptionException.class);
	}

}
