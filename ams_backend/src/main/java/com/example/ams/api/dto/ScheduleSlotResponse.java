package com.example.ams.api.dto;

import java.time.LocalTime;

import com.example.ams.domain.clazz.ClassScheduleSlot;
import com.example.ams.domain.clazz.DayOfWeek;

public record ScheduleSlotResponse(
		long scheduleId,
		DayOfWeek dayOfWeek,
		LocalTime startTime,
		LocalTime endTime,
		String room) {

	public static ScheduleSlotResponse from(ClassScheduleSlot slot) {
		return new ScheduleSlotResponse(
				slot.scheduleId(),
				slot.dayOfWeek(),
				slot.startTime(),
				slot.endTime(),
				slot.room());
	}
}
