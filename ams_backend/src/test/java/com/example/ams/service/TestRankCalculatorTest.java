package com.example.ams.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class TestRankCalculatorTest {

	@Test
	void computeRanks_tieSkipsNextRank() {
		var inputs = List.of(
				new TestRankCalculator.RankInput(1L, new BigDecimal("100")),
				new TestRankCalculator.RankInput(2L, new BigDecimal("100")),
				new TestRankCalculator.RankInput(3L, new BigDecimal("99")));
		Map<Long, Integer> ranks = TestRankCalculator.computeRanks(inputs);
		assertEquals(1, ranks.get(1L));
		assertEquals(1, ranks.get(2L));
		assertEquals(3, ranks.get(3L));
	}
}
