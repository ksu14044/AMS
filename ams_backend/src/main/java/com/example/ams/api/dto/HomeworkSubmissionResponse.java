package com.example.ams.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.example.ams.domain.clazz.HomeworkSubmission;
import com.example.ams.service.HomeworkService.SubmissionRow;

public record HomeworkSubmissionResponse(
		long studentId,
		String studentName,
		boolean submitted,
		Instant submittedAt,
		BigDecimal score,
		String grade,
		String memo) {

	public static HomeworkSubmissionResponse from(SubmissionRow row) {
		HomeworkSubmission s = row.submission();
		return new HomeworkSubmissionResponse(
				row.studentId(),
				row.studentName(),
				s.submitted(),
				s.submittedAt(),
				s.score(),
				s.grade(),
				s.memo());
	}
}
