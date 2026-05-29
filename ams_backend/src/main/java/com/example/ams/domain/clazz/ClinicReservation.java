package com.example.ams.domain.clazz;

import java.time.Instant;

public record ClinicReservation(
		long reservationId,
		long slotId,
		long studentId,
		String studentName,
		ClinicReservationStatus status,
		Boolean resultAttended,
		String resultMemo,
		Instant createdAt) {
}
