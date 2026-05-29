package com.example.ams.api.dto;

import java.time.Instant;

import com.example.ams.service.DiligenceReportService.ReportGenerationTargetRow;

public record ReportGenerationTargetResponse(
		long testId,
		String title,
		Instant testAt,
		Instant completedAt,
		boolean reportGenerated) {

	public static ReportGenerationTargetResponse from(ReportGenerationTargetRow row) {
		return new ReportGenerationTargetResponse(
				row.testId(),
				row.title(),
				row.testAt(),
				row.completedAt(),
				row.reportGenerated());
	}
}
