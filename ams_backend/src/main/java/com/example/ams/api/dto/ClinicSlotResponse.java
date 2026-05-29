package com.example.ams.api.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.example.ams.domain.clazz.ClinicSlot;
import com.example.ams.domain.clazz.DayOfWeek;

public record ClinicSlotResponse(
		long slotId,
		long classId,
		LocalDate weekStartDate,
		DayOfWeek dayOfWeek,
		LocalTime startTime,
		Long assistantId,
		String assistantName,
		int maxCapacity) {

	public static ClinicSlotResponse from(ClinicSlot slot) {
		return new ClinicSlotResponse(
				slot.slotId(),
				slot.classId(),
				slot.weekStartDate(),
				slot.dayOfWeek(),
				slot.startTime(),
				slot.assistantId(),
				slot.assistantName(),
				slot.maxCapacity());
	}
}
