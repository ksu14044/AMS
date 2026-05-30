package com.example.ams.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * [DECISIONS.md] §12 — 재시험 판정 (학생 점수 올림 vs 기준 점수 내림)
 */
public final class TestRetakeEvaluator {

	private TestRetakeEvaluator() {
	}

	/**
	 * @param retakeThresholdCount 합격에 필요한 최소 맞은 문항 수
	 */
	public static boolean needsRetake(BigDecimal rawScore, int questionCount, int retakeThresholdCount) {
		if (rawScore == null || questionCount <= 0 || retakeThresholdCount <= 0) {
			return false;
		}
		if (retakeThresholdCount > questionCount) {
			return false;
		}
		BigDecimal threshold = BigDecimal.valueOf(100)
				.multiply(BigDecimal.valueOf(retakeThresholdCount))
				.divide(BigDecimal.valueOf(questionCount), 10, RoundingMode.HALF_UP)
				.setScale(1, RoundingMode.FLOOR);
		BigDecimal student = rawScore.setScale(1, RoundingMode.CEILING);
		return student.compareTo(threshold) < 0;
	}

	public static BigDecimal passingThresholdScore(int questionCount, int retakeThresholdCount) {
		if (questionCount <= 0 || retakeThresholdCount <= 0) {
			return null;
		}
		return BigDecimal.valueOf(100)
				.multiply(BigDecimal.valueOf(retakeThresholdCount))
				.divide(BigDecimal.valueOf(questionCount), 10, RoundingMode.HALF_UP)
				.setScale(1, RoundingMode.FLOOR);
	}
}
