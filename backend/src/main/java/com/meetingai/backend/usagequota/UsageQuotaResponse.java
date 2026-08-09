package com.meetingai.backend.usagequota;

import java.util.UUID;

public record UsageQuotaResponse(
		UUID id,
		String monthReference,
		int meetingsUploaded,
		int meetingsLimit) {

	public static UsageQuotaResponse from(UsageQuota usageQuota) {
		return new UsageQuotaResponse(
				usageQuota.getId(),
				usageQuota.getMonthReference(),
				usageQuota.getMeetingsUploaded(),
				usageQuota.getMeetingsLimit());
	}

}
