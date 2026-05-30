package com.example.ams.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * [DECISIONS.md] §13 — 동점 공동 등수, 다음 등수 건너뜀 (100·100·99 → 1·1·3)
 */
public final class TestRankCalculator {

	private TestRankCalculator() {
	}

	public record RankInput(long studentId, BigDecimal rawScore) {
	}

	public static Map<Long, Integer> computeRanks(List<RankInput> inputs) {
		Map<Long, Integer> ranks = new HashMap<>();
		List<RankInput> scored = inputs.stream()
				.filter(i -> i.rawScore() != null)
				.sorted(Comparator.comparing(RankInput::rawScore).reversed())
				.toList();

		int rank = 1;
		int index = 0;
		while (index < scored.size()) {
			BigDecimal current = scored.get(index).rawScore();
			List<RankInput> tied = new ArrayList<>();
			while (index < scored.size() && scored.get(index).rawScore().compareTo(current) == 0) {
				tied.add(scored.get(index));
				index++;
			}
			for (RankInput input : tied) {
				ranks.put(input.studentId(), rank);
			}
			rank += tied.size();
		}
		return ranks;
	}
}
