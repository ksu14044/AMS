package com.example.ams.api.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateReportPeriodPresetRequest(
		@NotBlank @Size(max = 100) String name,
		@NotNull LocalDate periodStart,
		@NotNull LocalDate periodEnd) {
}
