package com.meetingai.backend.crm;

public class CrmConnectionException extends RuntimeException {

	public CrmConnectionException(String message) {
		super(message);
	}

	public CrmConnectionException(String message, Throwable cause) {
		super(message, cause);
	}

}
