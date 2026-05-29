package com.example.ams.domain.clazz;

import java.time.LocalDate;
import java.time.LocalTime;

public record ClinicSlot(
		long slotId,
		long classId,
		LocalDate weekStartDate,
		DayOfWeek dayOfWeek,
		LocalTime startTime,
		Long assistantId,
		String assistantName,
		int maxCapacity) {
}
