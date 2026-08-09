package com.meetingai.backend.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import com.meetingai.backend.user.User;
import com.meetingai.backend.user.UserRepository;

@Service
public class CurrentUserService {

	private final UserRepository userRepository;

	public CurrentUserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public User getCurrentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof OidcUser oidcUser)) {
			throw new IllegalStateException("Nenhum usuário autenticado no contexto de segurança");
		}
		return userRepository.findByGoogleSub(oidcUser.getSubject())
				.orElseThrow(() -> new IllegalStateException(
						"Usuário autenticado via Google não foi encontrado na base local"));
	}

}
