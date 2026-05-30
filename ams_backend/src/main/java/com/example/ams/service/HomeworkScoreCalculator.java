package com.example.ams.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class HomeworkScoreCalculator {

	private HomeworkScoreCalculator() {
	}

	public static BigDecimal computeScore(int questionCount, int correctCount) {
		if (questionCount <= 0) {
			return null;
		}
		BigDecimal ratio = BigDecimal.valueOf(100)
				.divide(BigDecimal.valueOf(questionCount), 10, RoundingMode.HALF_UP)
				.multiply(BigDecimal.valueOf(correctCount));
		return ratio.setScale(1, RoundingMode.CEILING);
	}

	public static int countCorrect(java.util.List<String> studentAnswers, java.util.List<String> correctAnswers) {
		int count = 0;
		int size = Math.min(studentAnswers.size(), correctAnswers.size());
		for (int i = 0; i < size; i++) {
			if (matches(studentAnswers.get(i), correctAnswers.get(i))) {
				count++;
			}
		}
		return count;
	}

	static boolean matches(String studentAnswer, String correctAnswer) {
		if (correctAnswer == null || correctAnswer.isBlank()) {
			return false;
		}
		if (studentAnswer == null || studentAnswer.isBlank()) {
			return false;
		}
		return normalize(studentAnswer).equals(normalize(correctAnswer));
	}

	private static String normalize(String value) {
		return value.trim();
	}
}
