package com.example.ams.api.dto;

import java.time.LocalDate;
import java.util.List;

import com.example.ams.service.ClinicReservationService.MyAssistantClinicWeekView;

public record MyAssistantClinicWeekResponse(LocalDate weekStart, List<MyAssistantClinicSlotResponse> slots) {

	public static MyAssistantClinicWeekResponse from(MyAssistantClinicWeekView view) {
		return new MyAssistantClinicWeekResponse(
				view.weekStart(),
				view.slots().stream().map(MyAssistantClinicSlotResponse::from).toList());
	}
}
