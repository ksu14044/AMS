package com.example.ams.api.dto;

import java.time.Instant;

import com.example.ams.service.DiligenceReportService.ReportListRow;

public record DiligenceReportListResponse(
		long reportId,
		Long testId,
		String testTitle,
		String periodLabel,
		long studentId,
		String studentName,
		Instant periodStart,
		Instant periodEnd,
		int totalScore,
		String overallGrade,
		Instant createdAt) {

	public static DiligenceReportListResponse from(ReportListRow row) {
		return new DiligenceReportListResponse(
				row.reportId(),
				row.testId(),
				row.testTitle(),
				row.periodLabel(),
				row.studentId(),
				row.studentName(),
				row.periodStart(),
				row.periodEnd(),
				row.totalScore(),
				row.overallGrade(),
				row.createdAt());
	}
}
