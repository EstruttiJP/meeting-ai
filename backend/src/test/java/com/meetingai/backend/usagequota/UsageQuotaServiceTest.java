package com.meetingai.backend.usagequota;

import java.time.YearMonth;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.meetingai.backend.user.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UsageQuotaServiceTest {

	private static final int DEFAULT_LIMIT = 10;

	@Mock
	private UsageQuotaRepository usageQuotaRepository;

	private UsageQuotaService usageQuotaService;
	private User user;
	private String currentMonth;

	@BeforeEach
	void setUp() {
		usageQuotaService = new UsageQuotaService(usageQuotaRepository, DEFAULT_LIMIT);
		user = new User("google-sub-1", "dev@meetingai.com", "Dev User", null);
		currentMonth = YearMonth.now().toString();
	}

	@Test
	void createsQuotaWithDefaultLimitWhenNoneExistsForCurrentMonth() {
		given(usageQuotaRepository.findByUserIdAndMonthReference(any(), any())).willReturn(Optional.empty());
		given(usageQuotaRepository.save(any(UsageQuota.class))).willAnswer(invocation -> invocation.getArgument(0));

		UsageQuota quota = usageQuotaService.getOrCreateCurrentMonthQuota(user);

		assertThat(quota.getMonthReference()).isEqualTo(currentMonth);
		assertThat(quota.getMeetingsLimit()).isEqualTo(DEFAULT_LIMIT);
		assertThat(quota.getMeetingsUploaded()).isZero();
	}

	@Test
	void reusesExistingQuotaForCurrentMonth() {
		UsageQuota existing = new UsageQuota(user, currentMonth, DEFAULT_LIMIT);
		given(usageQuotaRepository.findByUserIdAndMonthReference(user.getId(), currentMonth))
				.willReturn(Optional.of(existing));

		UsageQuota quota = usageQuotaService.getOrCreateCurrentMonthQuota(user);

		assertThat(quota).isSameAs(existing);
		verify(usageQuotaRepository, never()).save(any());
	}

	@Test
	void consumeUploadSlotIncrementsUsageWhenUnderLimit() {
		UsageQuota existing = new UsageQuota(user, currentMonth, DEFAULT_LIMIT);
		given(usageQuotaRepository.findByUserIdAndMonthReference(user.getId(), currentMonth))
				.willReturn(Optional.of(existing));
		given(usageQuotaRepository.save(any(UsageQuota.class))).willAnswer(invocation -> invocation.getArgument(0));

		usageQuotaService.consumeUploadSlot(user);

		assertThat(existing.getMeetingsUploaded()).isEqualTo(1);
		verify(usageQuotaRepository, times(1)).save(existing);
	}

	@Test
	void consumeUploadSlotThrowsAndDoesNotIncrementWhenAtLimit() {
		UsageQuota atLimit = new UsageQuota(user, currentMonth, 1);
		atLimit.incrementUsage();
		given(usageQuotaRepository.findByUserIdAndMonthReference(user.getId(), currentMonth))
				.willReturn(Optional.of(atLimit));

		assertThatThrownBy(() -> usageQuotaService.consumeUploadSlot(user))
				.isInstanceOf(UsageQuotaExceededException.class);
		assertThat(atLimit.getMeetingsUploaded()).isEqualTo(1);
		verify(usageQuotaRepository, never()).save(any());
	}

}
