package com.example.ams.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * [DECISIONS.md] §9 percentile_rank · upper_rank_pct
 */
public final class TestScoreCalculator {

	private TestScoreCalculator() {
	}

	public record ScoreInput(long studentId, BigDecimal rawScore) {
	}

	public record ScoreOutput(
			BigDecimal classAverage,
			BigDecimal classAvgPerStudent,
			Integer upperRankPct,
			Integer percentileRank) {
	}

	public static List<ScoreOutput> compute(List<ScoreInput> inputs) {
		if (inputs.isEmpty()) {
			return List.of();
		}

		List<ScoreInput> withScore = inputs.stream()
				.filter(i -> i.rawScore() != null)
				.toList();
		BigDecimal classAverage = null;
		if (!withScore.isEmpty()) {
			BigDecimal sum = withScore.stream()
					.map(ScoreInput::rawScore)
					.reduce(BigDecimal.ZERO, BigDecimal::add);
			classAverage = sum.divide(BigDecimal.valueOf(withScore.size()), 2, RoundingMode.HALF_UP);
		}

		List<ScoreOutput> results = new ArrayList<>();
		for (ScoreInput input : inputs) {
			if (input.rawScore() == null) {
				results.add(new ScoreOutput(classAverage, classAverage, null, 0));
				continue;
			}
			int lowerCount = 0;
			for (ScoreInput other : withScore) {
				if (other.rawScore().compareTo(input.rawScore()) < 0) {
					lowerCount++;
				}
			}
			int percentile = withScore.size() <= 1
					? 100
					: (int) Math.round((lowerCount * 100.0) / (withScore.size() - 1));
			int upperRank = 100 - percentile;
			results.add(new ScoreOutput(classAverage, classAverage, upperRank, percentile));
		}
		return results;
	}
}
