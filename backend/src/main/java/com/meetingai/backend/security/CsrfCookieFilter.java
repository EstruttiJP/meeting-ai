package com.meetingai.backend.security;

import java.io.IOException;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * CookieCsrfTokenRepository só escreve o cookie XSRF-TOKEN quando algo de fato lê o
 * token da requisição — por padrão isso é "deferred" (só acontece se um formulário
 * server-side renderizar o token). Numa SPA nada faz essa leitura sozinho, então o
 * cookie nunca aparecia e todo POST/PUT/DELETE do Angular caía num 403 silencioso
 * (sem corpo JSON, porque a rejeição de CSRF nem chega no ApiExceptionHandler).
 * Esse filtro força a resolução do token em toda requisição, garantindo que o
 * cookie já esteja lá desde a primeira chamada (ex.: GET /api/users/me no load).
 */
class CsrfCookieFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
		if (csrfToken != null) {
			csrfToken.getToken();
		}
		filterChain.doFilter(request, response);
	}

}
