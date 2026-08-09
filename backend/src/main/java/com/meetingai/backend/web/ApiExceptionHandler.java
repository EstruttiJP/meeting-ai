package com.meetingai.backend.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.meetingai.backend.meeting.MeetingFileTooLargeException;
import com.meetingai.backend.meeting.UnsupportedMeetingFormatException;
import com.meetingai.backend.usagequota.UsageQuotaExceededException;

/**
 * Traduz exceções de regra de negócio em respostas HTTP com mensagem clara.
 * Nunca deve expor detalhes internos (stack trace, chave de API, etc) — só
 * a mensagem de negócio já pensada para o usuário final.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(UsageQuotaExceededException.class)
	public ProblemDetail handleUsageQuotaExceeded(UsageQuotaExceededException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
	}

	@ExceptionHandler(UnsupportedMeetingFormatException.class)
	public ProblemDetail handleUnsupportedFormat(UnsupportedMeetingFormatException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex.getMessage());
	}

	@ExceptionHandler({ MeetingFileTooLargeException.class, MaxUploadSizeExceededException.class })
	public ProblemDetail handleFileTooLarge(Exception ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.PAYLOAD_TOO_LARGE,
				"Arquivo excede o tamanho máximo permitido.");
	}

}
