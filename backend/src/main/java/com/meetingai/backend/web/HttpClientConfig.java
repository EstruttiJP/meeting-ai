package com.meetingai.backend.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * spring-boot-starter-webmvc não traz a autoconfiguração de RestClient.Builder
 * (isso vem de um starter web mais completo) — expomos o bean manualmente
 * para os providers (Whisper, OpenRouter, Pipedrive) injetarem.
 */
@Configuration
public class HttpClientConfig {

	@Bean
	RestClient.Builder restClientBuilder() {
		return RestClient.builder();
	}

}
