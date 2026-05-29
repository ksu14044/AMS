package com.example.ams.api.dto;

import com.example.ams.domain.clazz.ClinicReservation;
import com.example.ams.domain.clazz.ClinicReservationStatus;

public record ClinicReservationResponse(
		long reservationId,
		long slotId,
		long studentId,
		String studentName,
		ClinicReservationStatus status,
		Boolean resultAttended,
		String resultMemo) {

	public static ClinicReservationResponse from(ClinicReservation r) {
		return new ClinicReservationResponse(
				r.reservationId(),
				r.slotId(),
				r.studentId(),
				r.studentName(),
				r.status(),
				r.resultAttended(),
				r.resultMemo());
	}
}
