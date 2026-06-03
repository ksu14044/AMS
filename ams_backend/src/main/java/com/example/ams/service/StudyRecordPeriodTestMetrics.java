package com.example.ams.service;

import java.math.BigDecimal;

/** 보고서·기간 집계용 테스트 지표 (기간 내 완료 시험, 루트별 최신 재시험 반영). */
public record StudyRecordPeriodTestMetrics(
		int averageScorePercent,
		boolean hasScoredTest,
		BigDecimal displayRawScore,
		BigDecimal latestClassAvg,
		Integer latestRank,
		Long representativeTestId) {

	public static StudyRecordPeriodTestMetrics empty() {
		return new StudyRecordPeriodTestMetrics(0, false, null, null, null, null);
	}
}
