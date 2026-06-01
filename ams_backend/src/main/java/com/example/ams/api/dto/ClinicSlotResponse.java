package com.example.ams.api.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.example.ams.domain.clazz.ClinicSlot;
import com.example.ams.domain.clazz.DayOfWeek;
import com.example.ams.service.AssignmentTargetService.TargetView;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ClinicSlotResponse(
		long slotId,
		long classId,
		LocalDate weekStartDate,
		DayOfWeek dayOfWeek,
		LocalTime startTime,
		Long assistantId,
		String assistantName,
		int maxCapacity,
		long presetId,
		String presetName,
		List<ClinicResultFieldResponse> resultFields,
		AssignmentTargetResponse targets) {

	public static ClinicSlotResponse from(ClinicSlot slot, TargetView targets, List<ClinicResultFieldResponse> fields) {
		return new ClinicSlotResponse(
				slot.slotId(),
				slot.classId(),
				slot.weekStartDate(),
				slot.dayOfWeek(),
				slot.startTime(),
				slot.assistantId(),
				slot.assistantName(),
				slot.maxCapacity(),
				slot.presetId(),
				slot.presetName(),
				fields,
				AssignmentTargetResponse.from(targets));
	}
}
