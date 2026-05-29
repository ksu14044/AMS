package com.example.ams.domain.clazz;

import java.time.Instant;

import com.example.ams.domain.user.Subject;

public record Clazz(
		long classId,
		long academyId,
		Subject subject,
		String name,
		long homeroomTeacherId,
		String classroom,
		Instant createdAt) {
}
