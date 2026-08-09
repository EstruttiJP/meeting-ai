package com.meetingai.backend.meeting;

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
class MeetingRepositoryTest {

	@Autowired
	private MeetingRepository meetingRepository;

	@Autowired
	private UserRepository userRepository;

	@Test
	void savesAndFindsMeetingsByUser() {
		User user = userRepository.saveAndFlush(
				new User("google-sub-meeting", "meeting@meetingai.com", "Meeting User", null));
		Meeting meeting = new Meeting(user, "Reunião de vendas", "reuniao.mp3", "meetings/reuniao.mp3");

		meetingRepository.saveAndFlush(meeting);

		List<Meeting> found = meetingRepository.findByUserId(user.getId());
		assertThat(found).hasSize(1);
		assertThat(found.get(0).getStatus()).isEqualTo(MeetingStatus.UPLOADED);
		assertThat(found.get(0).getUploadedAt()).isNotNull();
	}

}
