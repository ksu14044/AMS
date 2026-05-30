package com.example.ams.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 테스트 점수 저장 시 반 평균·석차 계산.
 */
public final class TestScoreCalculator {

	private TestScoreCalculator() {
	}

	public record ScoreInput(long studentId, BigDecimal rawScore) {
	}

	public record ScoreOutput(BigDecimal classAverage, BigDecimal classAvgPerStudent, Integer rank) {
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

		Map<Long, Integer> ranks = TestRankCalculator.computeRanks(withScore.stream()
				.map(i -> new TestRankCalculator.RankInput(i.studentId(), i.rawScore()))
				.toList());

		List<ScoreOutput> results = new ArrayList<>();
		for (ScoreInput input : inputs) {
			if (input.rawScore() == null) {
				results.add(new ScoreOutput(classAverage, classAverage, null));
				continue;
			}
			results.add(new ScoreOutput(classAverage, classAverage, ranks.get(input.studentId())));
		}
		return results;
	}
}
