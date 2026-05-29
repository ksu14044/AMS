package com.example.ams.domain.clazz;

import java.time.Instant;

public record ClassEnrollment(
		long enrollmentId,
		long classId,
		long studentId,
		Instant assignedAt,
		long assignedBy) {
}
