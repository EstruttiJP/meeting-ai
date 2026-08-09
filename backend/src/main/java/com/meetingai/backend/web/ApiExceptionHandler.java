package com.meetingai.backend.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.meetingai.backend.aiprovider.AiProviderConfigNotFoundException;
import com.meetingai.backend.crm.CrmConnectionException;
import com.meetingai.backend.meeting.MeetingFileTooLargeException;
import com.meetingai.backend.meeting.MeetingNotFoundException;
import com.meetingai.backend.meeting.UnsupportedMeetingFormatException;
import com.meetingai.backend.summary.SummaryNotApprovedException;
import com.meetingai.backend.summary.SummaryNotFoundException;
import com.meetingai.backend.transcription.TranscriptionNotFoundException;
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

	@ExceptionHandler(AiProviderConfigNotFoundException.class)
	public ProblemDetail handleAiProviderConfigNotFound(AiProviderConfigNotFoundException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(MeetingNotFoundException.class)
	public ProblemDetail handleMeetingNotFound(MeetingNotFoundException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(SummaryNotFoundException.class)
	public ProblemDetail handleSummaryNotFound(SummaryNotFoundException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(CrmConnectionException.class)
	public ProblemDetail handleCrmConnectionFailure(CrmConnectionException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	@ExceptionHandler(SummaryNotApprovedException.class)
	public ProblemDetail handleSummaryNotApproved(SummaryNotApprovedException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
	}

	@ExceptionHandler(TranscriptionNotFoundException.class)
	public ProblemDetail handleTranscriptionNotFound(TranscriptionNotFoundException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	/**
	 * Só campo + mensagem padrão do Bean Validation — nunca o valor rejeitado.
	 * Um dos campos validados é a chave de API própria do usuário
	 * (AiProviderConfigRequest.apiKey), que não pode aparecer em resposta de
	 * erro nenhuma, nem por acidente via getRejectedValue().
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
		List<String> errors = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.toList();
		ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Dados inválidos");
		detail.setProperty("errors", errors);
		return detail;
	}

}
