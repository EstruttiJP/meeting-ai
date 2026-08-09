package com.meetingai.backend.aiprovider;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import com.meetingai.backend.user.User;
import com.meetingai.backend.user.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AiProviderConfigRepositoryTest {

	@Autowired
	private AiProviderConfigRepository aiProviderConfigRepository;

	@Autowired
	private UserRepository userRepository;

	@Test
	void savesAndFindsConfigsByUser() {
		User user = userRepository.saveAndFlush(
				new User("google-sub-ai", "ai@meetingai.com", "AI User", null));
		AiProviderConfig config = new AiProviderConfig(user, AiProvider.OPENROUTER, null, true);

		aiProviderConfigRepository.saveAndFlush(config);

		List<AiProviderConfig> found = aiProviderConfigRepository.findByUserId(user.getId());
		assertThat(found).hasSize(1);
		assertThat(found.get(0).getProvider()).isEqualTo(AiProvider.OPENROUTER);
		assertThat(found.get(0).isDefault()).isTrue();
		assertThat(found.get(0).getApiKeyEncrypted()).isNull();
	}

}
