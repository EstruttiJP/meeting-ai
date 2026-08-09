package com.meetingai.backend.security;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

	private final AppOidcUserService appOidcUserService;
	private final String frontendUrl;

	public SecurityConfig(AppOidcUserService appOidcUserService,
			@Value("${app.frontend-url}") String frontendUrl) {
		this.appOidcUserService = appOidcUserService;
		this.frontendUrl = frontendUrl;
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.cors(cors -> cors.configurationSource(corsConfigurationSource()))
				// Login via OAuth2 usa cookie de sessão (não é mais uma API sem estado) —
				// CSRF protege via cookie legível pelo Angular (XSRF-TOKEN -> header X-XSRF-TOKEN).
				.csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/actuator/health", "/oauth2/**", "/login/**").permitAll()
						.requestMatchers("/api/**").authenticated()
						.anyRequest().authenticated())
				// Chamadas de API não devem ser redirecionadas para a página de login (não existe
				// uma, o Angular inicia o fluxo navegando direto para /oauth2/authorization/google).
				.exceptionHandling(exceptions -> exceptions.defaultAuthenticationEntryPointFor(
						new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
						PathPatternRequestMatcher.withDefaults().matcher("/api/**")))
				.oauth2Login(oauth2 -> oauth2
						.userInfoEndpoint(userInfo -> userInfo.oidcUserService(appOidcUserService))
						.successHandler(oauth2LoginSuccessHandler()));
		return http.build();
	}

	private SimpleUrlAuthenticationSuccessHandler oauth2LoginSuccessHandler() {
		SimpleUrlAuthenticationSuccessHandler handler = new SimpleUrlAuthenticationSuccessHandler(frontendUrl);
		handler.setAlwaysUseDefaultTargetUrl(true);
		return handler;
	}

	private CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(List.of(frontendUrl));
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("*"));
		configuration.setAllowCredentials(true);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

}
