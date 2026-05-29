package com.example.ams.domain.user;

import java.time.Instant;

public record User(
		long userId,
		long academyId,
		String email,
		String passwordHash,
		String name,
		String phoneNumber,
		UserRole role,
		Subject subject,
		UserStatus status,
		Instant personalInfoConsentAt,
		Instant createdAt,
		Instant updatedAt) {
}
