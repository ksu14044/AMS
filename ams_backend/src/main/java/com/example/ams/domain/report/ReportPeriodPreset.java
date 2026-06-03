package com.example.ams.domain.report;

import java.time.LocalDate;

public record ReportPeriodPreset(
		long presetId,
		long classId,
		String name,
		LocalDate periodStart,
		LocalDate periodEnd) {
}
