package com.meetingai.backend.transcription;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class WhisperTranscriptionProviderTest {

	private MockRestServiceServer mockServer;
	private WhisperTranscriptionProvider provider;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder().baseUrl("http://whisper.local:9000");
		mockServer = MockRestServiceServer.bindTo(builder).build();
		provider = new WhisperTranscriptionProvider(builder.build(), new ObjectMapper());
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
	void sendsAudioBytesInTheAudioFilePartWhisperExpects() {
		mockServer.expect(requestTo(containsString("/asr")))
				.andExpect(method(HttpMethod.POST))
				.andExpect(content().string(allOf(
						containsString("name=\"audio_file\""),
						containsString("filename=\"reuniao.mp3\""),
						containsString("conteudo-do-audio"))))
				.andRespond(withSuccess("""
						{"text": "ok", "language": "pt"}
						""", MediaType.APPLICATION_JSON));

		provider.transcribe(
				new ByteArrayInputStream("conteudo-do-audio".getBytes()), "reuniao.mp3", "audio/mpeg");

		mockServer.verify();
	}

	@Test
	void parsesJsonBodyEvenWhenWhisperLabelsItAsTextPlain() {
		mockServer.expect(requestTo(containsString("/asr")))
				.andRespond(withSuccess("""
						{"text": "Reunião sobre vendas", "language": "pt"}
						""", new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8)));

		TranscriptionResult result = provider.transcribe(
				new ByteArrayInputStream("audio".getBytes()), "reuniao.mp3", "audio/mpeg");

		assertThat(result.content()).isEqualTo("Reunião sobre vendas");
	}

	@Test
	void includesTruncatedRawBodyWhenWhisperReturnsNonJson() {
		mockServer.expect(requestTo(containsString("/asr")))
				.andRespond(withSuccess("<html>Bad Gateway</html>", MediaType.TEXT_PLAIN));

		assertThatThrownBy(() -> provider.transcribe(
				new ByteArrayInputStream("audio".getBytes()), "reuniao.mp3", "audio/mpeg"))
				.isInstanceOf(TranscriptionException.class)
				.hasMessageContaining("<html>Bad Gateway</html>");
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
