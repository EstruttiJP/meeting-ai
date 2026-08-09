package com.meetingai.backend.usagequota;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import com.meetingai.backend.user.User;
import com.meetingai.backend.user.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UsageQuotaRepositoryTest {

	@Autowired
	private UsageQuotaRepository usageQuotaRepository;

	@Autowired
	private UserRepository userRepository;

	@Test
	void savesAndFindsQuotaByUserAndMonth() {
		User user = userRepository.saveAndFlush(
				new User("google-sub-quota", "quota@meetingai.com", "Quota User", null));
		UsageQuota quota = new UsageQuota(user, "2026-08", 10);

		usageQuotaRepository.saveAndFlush(quota);

		Optional<UsageQuota> found = usageQuotaRepository.findByUserIdAndMonthReference(user.getId(), "2026-08");
		assertThat(found).isPresent();
		assertThat(found.get().getMeetingsLimit()).isEqualTo(10);
		assertThat(found.get().getMeetingsUploaded()).isZero();
		assertThat(found.get().getCreatedAt()).isNotNull();
	}

}
