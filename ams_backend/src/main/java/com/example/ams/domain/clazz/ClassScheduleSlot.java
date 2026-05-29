package com.example.ams.domain.clazz;

import java.time.LocalTime;

public record ClassScheduleSlot(
		long scheduleId,
		long classId,
		DayOfWeek dayOfWeek,
		LocalTime startTime,
		LocalTime endTime,
		String room) {
}
