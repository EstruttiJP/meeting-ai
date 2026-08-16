package com.meetingai.backend.security;

import java.util.function.Supplier;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * O handler padrão do Spring Security (XorCsrfTokenRequestAttributeHandler) exige que
 * o token venha mascarado (BREACH protection) tanto na leitura quanto na escrita — o
 * Angular manda o valor puro do cookie XSRF-TOKEN de volta no header, sem máscara.
 * Delega a escrita para o handler padrão, mas na leitura só desmascara quando o
 * header estiver presente; senão cai no handler simples (usado por CsrfCookieFilter
 * pra só forçar a resolução do token, não pra validar uma requisição de verdade).
 */
final class SpaCsrfTokenRequestHandler extends CsrfTokenRequestAttributeHandler {

	private final CsrfTokenRequestHandler delegate = new XorCsrfTokenRequestAttributeHandler();

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
		this.delegate.handle(request, response, csrfToken);
	}

	@Override
	public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
		String headerValue = request.getHeader(csrfToken.getHeaderName());
		return StringUtils.hasText(headerValue)
				? super.resolveCsrfTokenValue(request, csrfToken)
				: this.delegate.resolveCsrfTokenValue(request, csrfToken);
	}

}
