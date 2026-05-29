package com.example.ams.domain.clazz;

import java.math.BigDecimal;

public record TestScore(
		long scoreId,
		long testId,
		long studentId,
		BigDecimal rawScore,
		String grade,
		BigDecimal classAvg,
		Integer upperRankPct,
		Integer percentileRank) {
}
