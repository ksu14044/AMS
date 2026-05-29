package com.example.ams.domain.clazz;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/** 예약 + 슬롯 일정 (구간 집계용) */
public record ClinicReservationWithSlot(
		ClinicReservation reservation,
		LocalDate weekStartDate,
		DayOfWeek dayOfWeek,
		LocalTime startTime) {

	public Instant occurrenceAt() {
		return ClinicSlotOccurrence.occurrenceAt(weekStartDate, dayOfWeek, startTime);
	}
}
