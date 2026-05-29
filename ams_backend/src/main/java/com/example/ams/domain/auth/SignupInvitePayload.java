package com.example.ams.domain.auth;

import java.time.Instant;

import com.example.ams.domain.user.Subject;
import com.example.ams.domain.user.UserRole;

public record SignupInvitePayload(
		SignupInviteKind kind,
		Long academyId,
		String academyCode,
		UserRole role,
		Subject subject,
		Instant expiresAt) {
}
