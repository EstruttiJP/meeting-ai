package com.meetingai.backend.user;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

	@Autowired
	private UserRepository userRepository;

	@Test
	void savesAndFindsUserByGoogleSub() {
		User user = new User("google-sub-123", "dev@meetingai.com", "Dev User", "https://example.com/pic.png");

		userRepository.saveAndFlush(user);

		Optional<User> found = userRepository.findByGoogleSub("google-sub-123");
		assertThat(found).isPresent();
		assertThat(found.get().getEmail()).isEqualTo("dev@meetingai.com");
		assertThat(found.get().getId()).isNotNull();
		assertThat(found.get().getCreatedAt()).isNotNull();
	}

	@Test
	void findByEmailReturnsEmptyWhenNotFound() {
		Optional<User> found = userRepository.findByEmail("missing@meetingai.com");

		assertThat(found).isEmpty();
	}

}
