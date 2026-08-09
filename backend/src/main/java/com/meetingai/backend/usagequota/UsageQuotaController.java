package com.meetingai.backend.usagequota;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meetingai.backend.security.CurrentUserService;
import com.meetingai.backend.user.User;

@RestController
@RequestMapping("/api/usage-quota")
public class UsageQuotaController {

	private final CurrentUserService currentUserService;
	private final UsageQuotaService usageQuotaService;

	public UsageQuotaController(CurrentUserService currentUserService, UsageQuotaService usageQuotaService) {
		this.currentUserService = currentUserService;
		this.usageQuotaService = usageQuotaService;
	}

	@GetMapping("/me")
	public ResponseEntity<UsageQuotaResponse> me() {
		User user = currentUserService.getCurrentUser();
		UsageQuota quota = usageQuotaService.getOrCreateCurrentMonthQuota(user);
		return ResponseEntity.ok(UsageQuotaResponse.from(quota));
	}

}
