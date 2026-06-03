package com.example.ams.domain.clazz;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record HomeworkSubmission(
		long submissionId,
		long homeworkId,
		long studentId,
		boolean submitted,
		Instant submittedAt,
		BigDecimal score,
		String grade,
		String memo,
		List<String> answers,
		Integer correctCount,
		List<Integer> wrongQuestionNos,
		Instant completedAt) {
}
