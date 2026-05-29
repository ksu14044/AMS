package com.example.ams.api.dto;

/**
 * 테스트 항목: 진행률 = percentile_rank 평균(높을수록 우수). 응시/마감은 보조 표기.
 */
public record StudyRecordTestMetricResponse(
		int ratePercent,
		int attemptedCount,
		int closedCount,
		String latestSummary) {
}
