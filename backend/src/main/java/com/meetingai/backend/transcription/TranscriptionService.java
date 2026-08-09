package com.meetingai.backend.transcription;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.meetingai.backend.meeting.Meeting;
import com.meetingai.backend.meeting.MeetingAccessService;
import com.meetingai.backend.user.User;

@Service
public class TranscriptionService {

	private final MeetingAccessService meetingAccessService;
	private final TranscriptionRepository transcriptionRepository;

	public TranscriptionService(MeetingAccessService meetingAccessService, TranscriptionRepository transcriptionRepository) {
		this.meetingAccessService = meetingAccessService;
		this.transcriptionRepository = transcriptionRepository;
	}

	public TranscriptionResponse getForMeeting(User user, UUID meetingId) {
		Meeting meeting = meetingAccessService.getOwnedMeeting(user, meetingId);
		Transcription transcription = transcriptionRepository.findByMeetingId(meeting.getId())
				.orElseThrow(() -> new TranscriptionNotFoundException(meetingId));
		return TranscriptionResponse.from(transcription);
	}

}
