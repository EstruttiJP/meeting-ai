package com.meetingai.backend.security;

import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetingai.backend.user.User;
import com.meetingai.backend.user.UserRepository;

@Service
public class AppOidcUserService extends OidcUserService {

	private final UserRepository userRepository;

	public AppOidcUserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	@Transactional
	public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
		OidcUser oidcUser = super.loadUser(userRequest);
		String googleSub = oidcUser.getSubject();
		userRepository.findByGoogleSub(googleSub)
				.orElseGet(() -> userRepository.save(new User(
						googleSub,
						oidcUser.getEmail(),
						oidcUser.getFullName() != null ? oidcUser.getFullName() : oidcUser.getEmail(),
						oidcUser.getPicture())));
		return oidcUser;
	}

}
