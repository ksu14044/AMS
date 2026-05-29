package com.example.ams.api.dto;

import java.time.LocalTime;

import com.example.ams.domain.clazz.DayOfWeek;

import jakarta.validation.constraints.NotNull;

public record ScheduleSlotRequest(
		@NotNull DayOfWeek dayOfWeek,
		@NotNull LocalTime startTime,
		@NotNull LocalTime endTime,
		String room) {
}
