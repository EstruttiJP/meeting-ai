package com.meetingai.backend.meeting;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
		Meeting meeting = new Meeting(user, "Reunião de vendas", "reuniao.mp3", "meetings/reuniao.mp3", MeetingType.GENERICA);

		meetingRepository.saveAndFlush(meeting);

		List<Meeting> found = meetingRepository.findByUserId(user.getId());
		assertThat(found).hasSize(1);
		assertThat(found.get(0).getStatus()).isEqualTo(MeetingStatus.UPLOADED);
		assertThat(found.get(0).getUploadedAt()).isNotNull();
	}

	@Test
	void findsOverdueMeetingsExcludingAlreadyExpiredOnes() {
		User user = userRepository.saveAndFlush(
				new User("google-sub-expiration", "expiration@meetingai.com", "Expiration User", null));

		Meeting overdue = new Meeting(user, "Reunião vencida", "a.mp3", "meetings/a.mp3", MeetingType.GENERICA);
		overdue.scheduleExpiration(Instant.now().minus(1, ChronoUnit.DAYS));
		meetingRepository.saveAndFlush(overdue);

		Meeting notYetDue = new Meeting(user, "Reunião recente", "b.mp3", "meetings/b.mp3", MeetingType.GENERICA);
		notYetDue.scheduleExpiration(Instant.now().plus(5, ChronoUnit.DAYS));
		meetingRepository.saveAndFlush(notYetDue);

		Meeting alreadyExpired = new Meeting(user, "Reunião já expirada", "c.mp3", "meetings/c.mp3", MeetingType.GENERICA);
		alreadyExpired.scheduleExpiration(Instant.now().minus(10, ChronoUnit.DAYS));
		alreadyExpired.markExpired();
		meetingRepository.saveAndFlush(alreadyExpired);

		List<Meeting> found = meetingRepository.findByStatusNotAndExpiresAtBefore(MeetingStatus.EXPIRED, Instant.now());

		assertThat(found).extracting(Meeting::getId).containsExactly(overdue.getId());
	}

}
