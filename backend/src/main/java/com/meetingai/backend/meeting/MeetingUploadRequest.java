package com.meetingai.backend.meeting;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class MeetingUploadRequest {

	@NotBlank
	private String title;

	@NotNull
	private MultipartFile file;

	/** Escolhido antes de anexar o arquivo; muda a ênfase do prompt de extração. */
	@NotNull
	private MeetingType meetingType;

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public MultipartFile getFile() {
		return file;
	}

	public void setFile(MultipartFile file) {
		this.file = file;
	}

	public MeetingType getMeetingType() {
		return meetingType;
	}

	public void setMeetingType(MeetingType meetingType) {
		this.meetingType = meetingType;
	}

}
