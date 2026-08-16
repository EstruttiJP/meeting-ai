package com.meetingai.backend.transcription;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import com.meetingai.backend.meeting.Meeting;
import com.meetingai.backend.meeting.MeetingType;
import com.meetingai.backend.meeting.MeetingRepository;
import com.meetingai.backend.user.User;
import com.meetingai.backend.user.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TranscriptionRepositoryTest {

	@Autowired
	private TranscriptionRepository transcriptionRepository;

	@Autowired
	private MeetingRepository meetingRepository;

	@Autowired
	private UserRepository userRepository;

	@Test
	void savesAndFindsTranscriptionByMeeting() {
		User user = userRepository.saveAndFlush(
				new User("google-sub-transcription", "transcription@meetingai.com", "Transcription User", null));
		Meeting meeting = meetingRepository.saveAndFlush(
				new Meeting(user, "Reunião de vendas", "reuniao.mp3", "meetings/reuniao.mp3", MeetingType.GENERICA));
		Transcription transcription = new Transcription(meeting, "conteúdo transcrito", "pt-BR", "whisper-local", null);

		transcriptionRepository.saveAndFlush(transcription);

		Optional<Transcription> found = transcriptionRepository.findByMeetingId(meeting.getId());
		assertThat(found).isPresent();
		assertThat(found.get().getContent()).isEqualTo("conteúdo transcrito");
		assertThat(found.get().getCreatedAt()).isNotNull();
	}

}
