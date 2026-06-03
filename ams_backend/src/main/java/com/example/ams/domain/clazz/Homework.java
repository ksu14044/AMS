package com.example.ams.domain.clazz;

import java.time.Instant;

public record Homework(
		long homeworkId,
		long classId,
		String title,
		Integer questionCount,
		String answerKeyPdfPath,
		AssignmentStatus status,
		Instant createdAt) {
}
