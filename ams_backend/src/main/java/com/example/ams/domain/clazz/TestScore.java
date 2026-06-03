package com.example.ams.domain.clazz;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record TestScore(
		long scoreId,
		long testId,
		long studentId,
		BigDecimal rawScore,
		String grade,
		BigDecimal classAvg,
		Integer rank,
		Integer upperRankPct,
		Integer percentileRank,
		List<String> answers,
		Integer correctCount,
		List<Integer> wrongQuestionNos,
		Instant gradedAt) {
}
