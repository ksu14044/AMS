package com.example.ams.api.dto;

import java.math.BigDecimal;

import com.example.ams.domain.clazz.TestScore;
import com.example.ams.service.TestExamService.ScoreRow;

public record TestScoreResponse(
		long studentId,
		String studentName,
		BigDecimal rawScore,
		String grade,
		BigDecimal classAvg,
		Integer upperRankPct,
		Integer percentileRank) {

	public static TestScoreResponse from(ScoreRow row) {
		TestScore s = row.score();
		return new TestScoreResponse(
				row.studentId(),
				row.studentName(),
				s.rawScore(),
				s.grade(),
				s.classAvg(),
				s.upperRankPct(),
				s.percentileRank());
	}
}
