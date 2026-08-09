package com.meetingai.backend.meeting;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.meetingai.backend.user.User;

/**
 * Busca uma reunião conferindo que ela pertence ao usuário autenticado.
 * Quem não é dono recebe 404 (via {@link MeetingNotFoundException}), nunca
 * 403 — não revela que o id existe pra quem não devia nem saber disso.
 */
@Service
public class MeetingAccessService {

	private final MeetingRepository meetingRepository;

	public MeetingAccessService(MeetingRepository meetingRepository) {
		this.meetingRepository = meetingRepository;
	}

	public Meeting getOwnedMeeting(User user, UUID meetingId) {
		Meeting meeting = meetingRepository.findById(meetingId)
				.orElseThrow(() -> new MeetingNotFoundException(meetingId));
		if (!meeting.getUser().getId().equals(user.getId())) {
			throw new MeetingNotFoundException(meetingId);
		}
		return meeting;
	}

}
