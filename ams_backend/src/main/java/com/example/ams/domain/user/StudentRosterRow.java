package com.example.ams.domain.user;

import java.time.Instant;
import java.util.List;

public record StudentRosterRow(
		long userId,
		String name,
		String email,
		String phoneNumber,
		UserStatus status,
		Instant createdAt,
		List<EnrolledClass> classes) {

	public record EnrolledClass(long classId, String name, String subject) {
	}
}
