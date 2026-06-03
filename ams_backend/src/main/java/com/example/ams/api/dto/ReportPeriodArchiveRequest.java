package com.example.ams.api.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record ReportPeriodArchiveRequest(
		@NotNull LocalDate periodStart,
		@NotNull LocalDate periodEnd) {
}
