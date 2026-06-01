package com.example.ams.api.dto;

import java.util.List;

import com.example.ams.service.ClinicReservationService.AssistantClinicSlotItem;

public record MyAssistantClinicSlotResponse(
		long classId,
		String className,
		ClinicSlotResponse slot,
		int bookedCount,
		int maxCapacity,
		boolean full,
		List<ClinicReservationResponse> reservations) {

	public static MyAssistantClinicSlotResponse from(AssistantClinicSlotItem item) {
		return new MyAssistantClinicSlotResponse(
				item.classId(),
				item.className(),
				ClinicSlotResponse.from(item.slot(), item.targets(), item.resultFields()),
				item.bookedCount(),
				item.maxCapacity(),
				item.full(),
				item.reservations().stream().map(ClinicReservationResponse::from).toList());
	}
}
