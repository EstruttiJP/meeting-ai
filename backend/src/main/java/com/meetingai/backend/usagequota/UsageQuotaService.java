package com.meetingai.backend.usagequota;

import java.time.YearMonth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetingai.backend.user.User;

@Service
public class UsageQuotaService {

	private final UsageQuotaRepository usageQuotaRepository;
	private final int defaultMonthlyLimit;

	public UsageQuotaService(UsageQuotaRepository usageQuotaRepository,
			@Value("${app.usage-quota.default-monthly-limit}") int defaultMonthlyLimit) {
		this.usageQuotaRepository = usageQuotaRepository;
		this.defaultMonthlyLimit = defaultMonthlyLimit;
	}

	@Transactional
	public UsageQuota getOrCreateCurrentMonthQuota(User user) {
		String monthReference = currentMonthReference();
		return usageQuotaRepository.findByUserIdAndMonthReference(user.getId(), monthReference)
				.orElseGet(() -> usageQuotaRepository.save(new UsageQuota(user, monthReference, defaultMonthlyLimit)));
	}

	/**
	 * Reserva uma vaga de upload no mês corrente. Lança {@link UsageQuotaExceededException}
	 * sem consumir nada se o usuário já estiver no limite.
	 */
	@Transactional
	public void consumeUploadSlot(User user) {
		UsageQuota quota = getOrCreateCurrentMonthQuota(user);
		if (!quota.hasCapacity()) {
			throw new UsageQuotaExceededException(quota.getMeetingsLimit());
		}
		quota.incrementUsage();
		usageQuotaRepository.save(quota);
	}

	private String currentMonthReference() {
		return YearMonth.now().toString();
	}

}
