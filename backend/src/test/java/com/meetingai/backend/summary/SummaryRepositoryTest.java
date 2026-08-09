package com.meetingai.backend.summary;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import com.meetingai.backend.meeting.Meeting;
import com.meetingai.backend.meeting.MeetingRepository;
import com.meetingai.backend.user.User;
import com.meetingai.backend.user.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SummaryRepositoryTest {

	@Autowired
	private SummaryRepository summaryRepository;

	@Autowired
	private MeetingRepository meetingRepository;

	@Autowired
	private UserRepository userRepository;

	@Test
	void savesAndFindsSummaryByMeetingWithJsonbContent() {
		User user = userRepository.saveAndFlush(
				new User("google-sub-summary", "summary@meetingai.com", "Summary User", null));
		Meeting meeting = meetingRepository.saveAndFlush(
				new Meeting(user, "Reunião de vendas", "reuniao.mp3", "meetings/reuniao.mp3"));
		String json = """
				{"summary":"Cliente interessado no plano anual","decisions":["Enviar proposta"]}
				""".trim();
		Summary summary = new Summary(meeting, json);

		summaryRepository.saveAndFlush(summary);

		Optional<Summary> found = summaryRepository.findByMeetingId(meeting.getId());
		assertThat(found).isPresent();
		assertThat(found.get().getContent()).contains("Cliente interessado no plano anual");
		assertThat(found.get().isApproved()).isFalse();
		assertThat(found.get().getCreatedAt()).isNotNull();
	}

}
