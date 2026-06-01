package com.example.ams.api.dto;

import java.time.LocalTime;

import com.example.ams.domain.clazz.DayOfWeek;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateClinicSlotRequest(
		@NotNull DayOfWeek dayOfWeek,
		@NotNull LocalTime startTime,
		@NotNull Long assistantId,
		@NotNull Long presetId,
		@Min(1) @Max(20) Integer maxCapacity) {
}
