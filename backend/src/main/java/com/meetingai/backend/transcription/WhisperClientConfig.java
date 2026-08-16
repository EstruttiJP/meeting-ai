package com.meetingai.backend.transcription;

import java.net.http.HttpClient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
class WhisperClientConfig {

	/**
	 * HTTP/1.1 forçado: no default (HTTP/2) o JDK HttpClient manda
	 * {@code Upgrade: h2c} em texto claro, e o uvicorn do whisper-asr-webservice
	 * descarta o corpo da requisição nesse upgrade — o FastAPI do outro lado
	 * responde {@code 422 audio_file: Field required} mesmo com o multipart
	 * montado corretamente.
	 */
	@Bean
	RestClient whisperRestClient(RestClient.Builder restClientBuilder,
			@Value("${app.transcription.whisper.base-url}") String baseUrl) {
		return restClientBuilder
				.requestFactory(new JdkClientHttpRequestFactory(
						HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build()))
				.baseUrl(baseUrl)
				.build();
	}

}
