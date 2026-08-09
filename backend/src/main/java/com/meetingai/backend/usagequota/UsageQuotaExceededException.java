package com.meetingai.backend.usagequota;

public class UsageQuotaExceededException extends RuntimeException {

	public UsageQuotaExceededException(int meetingsLimit) {
		super("Limite mensal de %d reuniões atingido. Aguarde o próximo mês ou faça upgrade de plano."
				.formatted(meetingsLimit));
	}

}
