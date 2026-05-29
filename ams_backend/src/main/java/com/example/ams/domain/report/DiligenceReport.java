package com.example.ams.domain.report;

import java.math.BigDecimal;
import java.time.Instant;

public record DiligenceReport(
		long reportId,
		long classId,
		long studentId,
		long testId,
		Instant periodStart,
		Instant periodEnd,
		int homeworkSubmitted,
		int homeworkTotal,
		Integer homeworkRate,
		String homeworkGrade,
		int clinicAttended,
		int clinicTotal,
		Integer clinicRate,
		String clinicGrade,
		BigDecimal testRawScore,
		BigDecimal testClassAvg,
		Integer testUpperRankPct,
		Integer testPercentileRank,
		String testGrade,
		int videoCertified,
		int videoTotal,
		Integer videoRate,
		String videoGrade,
		int totalScore,
		String overallGrade,
		String teacherComment,
		String pdfPath,
		Instant createdAt) {
}
