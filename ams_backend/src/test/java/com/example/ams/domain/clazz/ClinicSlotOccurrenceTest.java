package com.example.ams.domain.clazz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class ClinicSlotOccurrenceTest {

	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

	@Test
	void occurrenceAt_weekStartMondayAndWednesday() {
		LocalDate weekStart = LocalDate.of(2026, 5, 25);
		Instant expected = LocalDate.of(2026, 5, 27)
				.atTime(14, 0)
				.atZone(SEOUL)
				.toInstant();

		assertEquals(expected, ClinicSlotOccurrence.occurrenceAt(weekStart, DayOfWeek.WED, LocalTime.of(14, 0)));
	}

	@Test
	void isInPeriod_includesSlotAfterEnrollment_sameWeekStartBeforeAssignDate() {
		Instant periodStart = LocalDate.of(2026, 5, 26).atTime(15, 45).atZone(SEOUL).toInstant();
		Instant periodEnd = LocalDate.of(2026, 5, 28).atTime(13, 0).atZone(SEOUL).toInstant();
		LocalDate weekStart = LocalDate.of(2026, 5, 25);

		assertTrue(ClinicSlotOccurrence.isInPeriod(
				weekStart, DayOfWeek.WED, LocalTime.of(14, 0), periodStart, periodEnd));
	}

	@Test
	void isInPeriod_excludesSlotBeforeEnrollmentOnSameDay() {
		Instant periodStart = LocalDate.of(2026, 5, 26).atTime(15, 45).atZone(SEOUL).toInstant();
		Instant periodEnd = LocalDate.of(2026, 5, 28).atTime(13, 0).atZone(SEOUL).toInstant();
		LocalDate weekStart = LocalDate.of(2026, 5, 25);

		assertFalse(ClinicSlotOccurrence.isInPeriod(
				weekStart, DayOfWeek.TUE, LocalTime.of(10, 0), periodStart, periodEnd));
	}

	@Test
	void isInPeriod_boundaryInclusive() {
		Instant start = LocalDate.of(2026, 5, 27).atTime(14, 0).atZone(SEOUL).toInstant();
		Instant end = start;

		assertTrue(ClinicSlotOccurrence.isInPeriod(
				LocalDate.of(2026, 5, 25), DayOfWeek.WED, LocalTime.of(14, 0), start, end));
	}
}
