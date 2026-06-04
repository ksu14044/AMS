package com.example.ams.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.example.ams.domain.clazz.AssignmentStatus;
import com.example.ams.domain.clazz.TestExam;
import com.example.ams.service.AssignmentTargetService.TargetView;

public record TestExamResponse(
		long testId,
		long classId,
		String title,
		Instant testAt,
		AssignmentStatus status,
		BigDecimal classAverage,
		Instant completedAt,
		Integer questionCount,
		Integer retakeThresholdCount,
		Long parentTestId,
		int retakeAttemptNo,
		long rootTestId,
		boolean countOnlyGrading,
		int pendingGradeCount,
		AssignmentTargetResponse targets) {

	public static TestExamResponse from(
			TestExam test,
			TargetView targets,
			boolean countOnlyGrading,
			int pendingGradeCount) {
		return new TestExamResponse(
				test.testId(),
				test.classId(),
				test.title(),
				test.testAt(),
				test.status(),
				test.classAverage(),
				test.completedAt(),
				test.questionCount(),
				test.retakeThresholdCount(),
				test.parentTestId(),
				test.retakeAttemptNo(),
				test.rootTestId(),
				countOnlyGrading,
				pendingGradeCount,
				AssignmentTargetResponse.from(targets));
	}
}
