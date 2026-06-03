package com.example.ams.api.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record GenerateReportsRequest(
		@NotNull LocalDate periodStart,
		@NotNull LocalDate periodEnd,
		@NotEmpty List<Long> studentIds,
		Long presetId) {
}
