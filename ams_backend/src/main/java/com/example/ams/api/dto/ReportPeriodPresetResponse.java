package com.example.ams.api.dto;

import java.time.LocalDate;

import com.example.ams.domain.report.ReportPeriodPreset;

public record ReportPeriodPresetResponse(
		long presetId,
		String name,
		LocalDate periodStart,
		LocalDate periodEnd) {

	public static ReportPeriodPresetResponse from(ReportPeriodPreset preset) {
		return new ReportPeriodPresetResponse(
				preset.presetId(),
				preset.name(),
				preset.periodStart(),
				preset.periodEnd());
	}
}
