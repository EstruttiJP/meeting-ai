package com.meetingai.backend.summary;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetingai.backend.meeting.Meeting;
import com.meetingai.backend.meeting.MeetingNotFoundException;
import com.meetingai.backend.meeting.MeetingRepository;
import com.meetingai.backend.user.User;

import tools.jackson.databind.ObjectMapper;

/**
 * Leitura e edição/aprovação do resumo — o PUT é a própria revisão humana
 * (edita e aprova numa tacada só, já que não existe endpoint de "aprovar"
 * separado no contrato de API). Nunca envia nada ao CRM por conta própria.
 */
@Service
public class SummaryService {

	private final SummaryRepository summaryRepository;
	private final MeetingRepository meetingRepository;
	private final ObjectMapper objectMapper;

	public SummaryService(SummaryRepository summaryRepository, MeetingRepository meetingRepository, ObjectMapper objectMapper) {
		this.summaryRepository = summaryRepository;
		this.meetingRepository = meetingRepository;
		this.objectMapper = objectMapper;
	}

	@Transactional(readOnly = true)
	public SummaryResponse getForMeeting(User user, UUID meetingId) {
		Meeting meeting = getOwnedMeeting(user, meetingId);
		return toResponse(getSummaryOrThrow(meeting.getId()));
	}

	@Transactional
	public SummaryResponse approve(User user, UUID meetingId, SummaryContent content) {
		Meeting meeting = getOwnedMeeting(user, meetingId);
		Summary summary = getSummaryOrThrow(meeting.getId());
		summary.updateContent(objectMapper.writeValueAsString(content));
		summary.approve();
		return toResponse(summaryRepository.save(summary));
	}

	private Summary getSummaryOrThrow(UUID meetingId) {
		return summaryRepository.findByMeetingId(meetingId)
				.orElseThrow(() -> new SummaryNotFoundException(meetingId));
	}

	private Meeting getOwnedMeeting(User user, UUID meetingId) {
		Meeting meeting = meetingRepository.findById(meetingId)
				.orElseThrow(() -> new MeetingNotFoundException(meetingId));
		if (!meeting.getUser().getId().equals(user.getId())) {
			// Não revela pra quem não é dono que o id existe.
			throw new MeetingNotFoundException(meetingId);
		}
		return meeting;
	}

	private SummaryResponse toResponse(Summary summary) {
		SummaryContent content = objectMapper.readValue(summary.getContent(), SummaryContent.class);
		return new SummaryResponse(
				summary.getId(),
				summary.getMeeting().getId(),
				content,
				summary.isApproved(),
				summary.getApprovedAt());
	}

}
