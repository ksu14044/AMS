package com.example.ams.api.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record LessonRecordClinicItem(
		@NotNull LocalDate clinicDate,
		@NotNull LocalTime startTime,
		@NotNull Long assistantId,
		@Min(1) @Max(20) Integer maxCapacity) {

	public int resolvedMaxCapacity() {
		return maxCapacity != null ? maxCapacity : 10;
	}
}
