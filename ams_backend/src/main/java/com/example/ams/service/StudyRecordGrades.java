package com.example.ams.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** 공부기록·보고서 공통 등급 환산 */
public final class StudyRecordGrades {

	private StudyRecordGrades() {
	}

	public static int percent(int numerator, int denominator) {
		if (denominator == 0) {
			return 0;
		}
		return (int) Math.round((numerator * 100.0) / denominator);
	}

	/** 분모 0이면 null (해당 없음) */
	public static Integer rateOrNull(int numerator, int denominator) {
		if (denominator == 0) {
			return null;
		}
		return percent(numerator, denominator);
	}

	public static String letterGrade(int ratePercent) {
		if (ratePercent >= 90) {
			return "A";
		}
		if (ratePercent >= 75) {
			return "B";
		}
		if (ratePercent >= 60) {
			return "C";
		}
		if (ratePercent >= 50) {
			return "D";
		}
		return "F";
	}

	public static String letterGradeOrNull(Integer ratePercent) {
		if (ratePercent == null) {
			return null;
		}
		return letterGrade(ratePercent);
	}

	/** 보고서 종합용: 원점수를 0~100 만점 비율로 환산 (미입력 0). */
	public static int rawScorePercent(BigDecimal rawScore) {
		if (rawScore == null) {
			return 0;
		}
		int rounded = rawScore.setScale(0, RoundingMode.HALF_UP).intValue();
		return Math.max(0, Math.min(100, rounded));
	}

	/**
	 * 종합: 데이터 있는 항목만 가중치 합산 후 비율로 환산.
	 * 테스트(0.3)는 보고서 트리거 시험이므로 항상 포함.
	 */
	public static int weightedTotalPercent(
			Integer homeworkRate,
			Integer clinicRate,
			int testScorePercent,
			boolean includeTest) {
		double weightedSum = 0;
		double weightTotal = 0;
		if (homeworkRate != null) {
			weightedSum += homeworkRate * 0.4;
			weightTotal += 0.4;
		}
		if (clinicRate != null) {
			weightedSum += clinicRate * 0.3;
			weightTotal += 0.3;
		}
		if (includeTest) {
			weightedSum += testScorePercent * 0.3;
			weightTotal += 0.3;
		}
		if (weightTotal <= 0) {
			return 0;
		}
		return (int) Math.round(weightedSum / weightTotal);
	}
}
