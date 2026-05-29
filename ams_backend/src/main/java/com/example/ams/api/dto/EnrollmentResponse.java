package com.example.ams.api.dto;

import java.time.Instant;

import com.example.ams.domain.clazz.ClassEnrollment;

public record EnrollmentResponse(
		long enrollmentId,
		long classId,
		long studentId,
		Instant assignedAt,
		long assignedBy) {

	public static EnrollmentResponse from(ClassEnrollment enrollment) {
		return new EnrollmentResponse(
				enrollment.enrollmentId(),
				enrollment.classId(),
				enrollment.studentId(),
				enrollment.assignedAt(),
				enrollment.assignedBy());
	}
}
