package com.example.ams.api.dto;

import java.util.List;

import com.example.ams.service.ClinicReservationService.SlotBookingView;

public record ClinicSlotBookingResponse(
		ClinicSlotResponse slot,
		int bookedCount,
		int maxCapacity,
		boolean full,
		Long myReservationId,
		boolean studentTimeConflict,
		List<ClinicReservationResponse> reservations) {

	public static ClinicSlotBookingResponse from(SlotBookingView view) {
		return new ClinicSlotBookingResponse(
				ClinicSlotResponse.from(view.slot()),
				view.bookedCount(),
				view.maxCapacity(),
				view.full(),
				view.myReservationId(),
				view.studentTimeConflict(),
				view.reservations().stream().map(ClinicReservationResponse::from).toList());
	}
}
