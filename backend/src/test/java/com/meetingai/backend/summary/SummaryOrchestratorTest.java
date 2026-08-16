package com.meetingai.backend.summary;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.meetingai.backend.aiprovider.AiProvider;
import com.meetingai.backend.meeting.MeetingType;
import com.meetingai.backend.transcription.TranscriptionResult;
import com.meetingai.backend.aiprovider.AiProviderConfig;
import com.meetingai.backend.aiprovider.AiProviderConfigRepository;
import com.meetingai.backend.crypto.EncryptionService;
import com.meetingai.backend.user.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SummaryOrchestratorTest {

	@Mock
	private OpenRouterSummaryProvider defaultProvider;

	@Mock
	private AiProviderConfigRepository aiProviderConfigRepository;

	@Mock
	private EncryptionService encryptionService;

	@Mock
	private UserKeySummaryProvider openAiProvider;

	@Mock
	private UserKeySummaryProvider geminiProvider;

	private static final TranscriptionResult TRANSCRICAO =
			new TranscriptionResult("transcrição", "pt", List.of());

	private User user;
	private SummaryContent expectedContent;

	@BeforeEach
	void setUp() {
		user = new User("google-sub-1", "dev@meetingai.com", "Dev User", null);
		expectedContent = new SummaryContent("resumo", List.of());
	}

	private SummaryOrchestrator orchestratorWith(UserKeySummaryProvider... providers) {
		given(openAiProvider.supportedProvider()).willReturn(AiProvider.OPENAI);
		given(geminiProvider.supportedProvider()).willReturn(AiProvider.GEMINI);
		return new SummaryOrchestrator(defaultProvider, List.of(providers), aiProviderConfigRepository, encryptionService);
	}

	@Test
	void usesDefaultProviderWhenUserHasNoAiProviderConfig() {
		given(aiProviderConfigRepository.findByUserId(user.getId())).willReturn(List.of());
		given(defaultProvider.summarize(TRANSCRICAO, MeetingType.GENERICA)).willReturn(expectedContent);
		SummaryOrchestrator orchestrator = orchestratorWith(openAiProvider, geminiProvider);

		SummaryContent result = orchestrator.summarize(user, TRANSCRICAO, MeetingType.GENERICA);

		assertThat(result).isEqualTo(expectedContent);
	}

	@Test
	void usesDefaultProviderWhenDefaultConfigIsOpenRouter() {
		AiProviderConfig config = new AiProviderConfig(user, AiProvider.OPENROUTER, null, true);
		given(aiProviderConfigRepository.findByUserId(user.getId())).willReturn(List.of(config));
		given(defaultProvider.summarize(TRANSCRICAO, MeetingType.GENERICA)).willReturn(expectedContent);
		SummaryOrchestrator orchestrator = orchestratorWith(openAiProvider, geminiProvider);

		SummaryContent result = orchestrator.summarize(user, TRANSCRICAO, MeetingType.GENERICA);

		assertThat(result).isEqualTo(expectedContent);
	}

	@Test
	void usesMatchingUserKeyProviderWithDecryptedKeyWhenConfigured() {
		AiProviderConfig config = new AiProviderConfig(user, AiProvider.GEMINI, "encrypted-key", true);
		given(aiProviderConfigRepository.findByUserId(user.getId())).willReturn(List.of(config));
		given(encryptionService.decrypt("encrypted-key")).willReturn("plain-key");
		given(geminiProvider.summarize(TRANSCRICAO, MeetingType.GENERICA, "plain-key")).willReturn(expectedContent);
		SummaryOrchestrator orchestrator = orchestratorWith(openAiProvider, geminiProvider);

		SummaryContent result = orchestrator.summarize(user, TRANSCRICAO, MeetingType.GENERICA);

		assertThat(result).isEqualTo(expectedContent);
		verify(defaultProvider, never()).summarize(any(), any());
	}

	@Test
	void ignoresNonDefaultConfigsAndFallsBackToDefaultProvider() {
		AiProviderConfig nonDefault = new AiProviderConfig(user, AiProvider.GEMINI, "encrypted-key", false);
		given(aiProviderConfigRepository.findByUserId(user.getId())).willReturn(List.of(nonDefault));
		given(defaultProvider.summarize(TRANSCRICAO, MeetingType.GENERICA)).willReturn(expectedContent);
		SummaryOrchestrator orchestrator = orchestratorWith(openAiProvider, geminiProvider);

		orchestrator.summarize(user, TRANSCRICAO, MeetingType.GENERICA);

		verify(geminiProvider, never()).summarize(any(), any(), any());
	}

	@Test
	void throwsWhenDefaultConfigProviderHasNoRegisteredImplementation() {
		AiProviderConfig config = new AiProviderConfig(user, AiProvider.CLAUDE, "encrypted-key", true);
		given(aiProviderConfigRepository.findByUserId(user.getId())).willReturn(List.of(config));
		SummaryOrchestrator orchestrator = orchestratorWith(openAiProvider, geminiProvider);

		assertThatThrownBy(() -> orchestrator.summarize(user, TRANSCRICAO, MeetingType.GENERICA))
				.isInstanceOf(SummaryGenerationException.class);
	}

}
