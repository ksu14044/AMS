package com.example.ams.domain.clazz;

import java.time.Instant;
import java.time.LocalDate;

public record ClassEnrollment(
		long enrollmentId,
		long classId,
		long studentId,
		Instant assignedAt,
		long assignedBy,
		LocalDate accessibleFrom) {
}
