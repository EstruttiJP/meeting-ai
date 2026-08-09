package com.meetingai.backend.crm;

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
class CrmConnectionRepositoryTest {

	@Autowired
	private CrmConnectionRepository crmConnectionRepository;

	@Autowired
	private UserRepository userRepository;

	@Test
	void savesAndFindsConnectionByUser() {
		User user = userRepository.saveAndFlush(
				new User("google-sub-crm", "crm@meetingai.com", "CRM User", null));
		CrmConnection connection = new CrmConnection(user, CrmProvider.PIPEDRIVE, "token-abc", "refresh-abc", null);

		crmConnectionRepository.saveAndFlush(connection);

		Optional<CrmConnection> found = crmConnectionRepository.findByUserId(user.getId());
		assertThat(found).isPresent();
		assertThat(found.get().getProvider()).isEqualTo(CrmProvider.PIPEDRIVE);
		assertThat(found.get().getConnectedAt()).isNotNull();
	}

}
