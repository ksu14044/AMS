package com.example.ams.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.example.ams.service.DiligenceReportService.ReportDetailRow;

public record DiligenceReportDetailResponse(
		long reportId,
		long classId,
		String className,
		long studentId,
		String studentName,
		Long testId,
		String testTitle,
		String periodLabel,
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
		Integer testRank,
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

	public static DiligenceReportDetailResponse from(ReportDetailRow row) {
		return new DiligenceReportDetailResponse(
				row.reportId(),
				row.classId(),
				row.className(),
				row.studentId(),
				row.studentName(),
				row.testId(),
				row.testTitle(),
				row.periodLabel(),
				row.periodStart(),
				row.periodEnd(),
				row.homeworkSubmitted(),
				row.homeworkTotal(),
				row.homeworkRate(),
				row.homeworkGrade(),
				row.clinicAttended(),
				row.clinicTotal(),
				row.clinicRate(),
				row.clinicGrade(),
				row.testRawScore(),
				row.testClassAvg(),
				row.testUpperRankPct(),
				row.testPercentileRank(),
				row.testRank(),
				row.testGrade(),
				row.videoCertified(),
				row.videoTotal(),
				row.videoRate(),
				row.videoGrade(),
				row.totalScore(),
				row.overallGrade(),
				row.teacherComment(),
				row.pdfPath(),
				row.createdAt());
	}
}
