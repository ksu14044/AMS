package com.example.ams.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.example.ams.domain.clazz.AssignmentStatus;
import com.example.ams.domain.clazz.TestExam;

public record TestExamResponse(
		long testId,
		long classId,
		String title,
		Instant testAt,
		AssignmentStatus status,
		BigDecimal classAverage,
		Instant completedAt) {

	public static TestExamResponse from(TestExam test) {
		return new TestExamResponse(
				test.testId(),
				test.classId(),
				test.title(),
				test.testAt(),
				test.status(),
				test.classAverage(),
				test.completedAt());
	}
}
