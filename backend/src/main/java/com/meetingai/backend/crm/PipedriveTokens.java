package com.meetingai.backend.crm;

import java.time.Instant;

record PipedriveTokens(String accessToken, String refreshToken, Instant expiresAt) {
}
