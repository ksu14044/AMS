package com.example.ams.api.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.example.ams.domain.clazz.ClinicSlot;
import com.example.ams.domain.clazz.DayOfWeek;
import com.example.ams.service.AssignmentTargetService.TargetView;

public record ClinicSlotResponse(
		long slotId,
		long classId,
		LocalDate weekStartDate,
		DayOfWeek dayOfWeek,
		LocalTime startTime,
		Long assistantId,
		String assistantName,
		int maxCapacity,
		AssignmentTargetResponse targets) {

	public static ClinicSlotResponse from(ClinicSlot slot, TargetView targets) {
		return new ClinicSlotResponse(
				slot.slotId(),
				slot.classId(),
				slot.weekStartDate(),
				slot.dayOfWeek(),
				slot.startTime(),
				slot.assistantId(),
				slot.assistantName(),
				slot.maxCapacity(),
				AssignmentTargetResponse.from(targets));
	}
}
