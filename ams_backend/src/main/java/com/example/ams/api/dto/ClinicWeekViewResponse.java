package com.example.ams.api.dto;

import java.time.LocalDate;
import java.util.List;

import com.example.ams.domain.clazz.ClinicWeekStatus;
import com.example.ams.service.ClinicReservationService.ClinicWeekView;

public record ClinicWeekViewResponse(
		LocalDate weekStart,
		ClinicWeekStatus weekStatus,
		boolean bookingOpen,
		boolean withinBookingWindow,
		List<ClinicSlotBookingResponse> slots) {

	public static ClinicWeekViewResponse from(ClinicWeekView view) {
		return new ClinicWeekViewResponse(
				view.weekStart(),
				view.weekStatus(),
				view.bookingOpen(),
				view.withinBookingWindow(),
				view.slots().stream().map(ClinicSlotBookingResponse::from).toList());
	}
}
