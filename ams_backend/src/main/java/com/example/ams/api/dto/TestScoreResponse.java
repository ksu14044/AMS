package com.example.ams.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.example.ams.domain.clazz.AssignmentStatus;
import com.example.ams.domain.clazz.TestExam;
import com.example.ams.domain.clazz.TestScore;
import com.example.ams.service.TestExamService.ScoreRow;
import com.example.ams.service.TestRetakeEvaluator;

public record TestScoreResponse(
		long studentId,
		String studentName,
		BigDecimal rawScore,
		String grade,
		BigDecimal classAvg,
		Integer rank,
		Integer correctCount,
		List<Integer> wrongQuestionNos,
		Instant gradedAt,
		Boolean needsRetake) {

	public static TestScoreResponse from(ScoreRow row, TestExam test) {
		TestScore s = row.score();
		return new TestScoreResponse(
				row.studentId(),
				row.studentName(),
				s.rawScore(),
				s.grade(),
				s.classAvg(),
				s.rank(),
				s.correctCount(),
				s.wrongQuestionNos(),
				s.gradedAt(),
				resolveNeedsRetake(test, s));
	}

	private static Boolean resolveNeedsRetake(TestExam test, TestScore score) {
		if (test.status() != AssignmentStatus.COMPLETED) {
			return null;
		}
		if (test.questionCount() == null || test.retakeThresholdCount() == null || score.rawScore() == null) {
			return null;
		}
		return TestRetakeEvaluator.needsRetake(
				score.rawScore(),
				test.questionCount(),
				test.retakeThresholdCount());
	}
}
