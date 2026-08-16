package com.meetingai.backend.summary;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.meetingai.backend.aiprovider.AiProvider;

import com.meetingai.backend.meeting.MeetingType;
import com.meetingai.backend.transcription.TranscriptionResult;

import jakarta.validation.Validator;
import tools.jackson.databind.ObjectMapper;

/** Chave própria do usuário para a API da OpenAI (GPT). */
@Service
public class OpenAiSummaryProvider extends AbstractLlmSummaryProvider implements UserKeySummaryProvider {

	private final OpenAiCompatibleChatClient client;
	private final String model;

	public OpenAiSummaryProvider(RestClient.Builder restClientBuilder, ObjectMapper objectMapper, Validator validator,
			@Value("${app.summary.openai.base-url}") String baseUrl,
			@Value("${app.summary.openai.model}") String model) {
		super(objectMapper, validator);
		this.client = new OpenAiCompatibleChatClient(restClientBuilder, baseUrl);
		this.model = model;
	}

	@Override
	public AiProvider supportedProvider() {
		return AiProvider.OPENAI;
	}

	@Override
	public SummaryContent summarize(TranscriptionResult transcription, MeetingType meetingType, String apiKey) {
		String raw = client.complete(apiKey, model, buildPrompt(transcription, meetingType));
		return parseAndValidate(raw, transcription.segments());
	}

}
