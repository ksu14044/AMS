package com.example.ams.domain.clazz;

import java.math.BigDecimal;
import java.time.Instant;

public record TestExam(
		long testId,
		long classId,
		String title,
		Instant testAt,
		AssignmentStatus status,
		BigDecimal classAverage,
		Instant completedAt,
		Instant createdAt,
		Integer questionCount,
		Integer retakeThresholdCount,
		String answerKeyPdfPath,
		Long parentTestId,
		int retakeAttemptNo) {

	public boolean isRetake() {
		return parentTestId != null;
	}

	public long rootTestId() {
		return parentTestId != null ? parentTestId : testId;
	}
}
