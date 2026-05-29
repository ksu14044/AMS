package com.example.ams.domain.clazz;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * 클리닉 슬롯의 실제 일시 (주차 월요일 + 요일 + 시작 시각, Asia/Seoul).
 * 보고서·구간 집계는 {@code week_start_date}가 아니 이 시각으로 구간에 포함 여부를 판단한다.
 */
public final class ClinicSlotOccurrence {

	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

	private ClinicSlotOccurrence() {
	}

	public static LocalDate slotDate(LocalDate weekStartMonday, DayOfWeek dayOfWeek) {
		return weekStartMonday.plusDays(dayOfWeek.ordinal());
	}

	public static Instant occurrenceAt(LocalDate weekStartMonday, DayOfWeek dayOfWeek, LocalTime startTime) {
		return slotDate(weekStartMonday, dayOfWeek).atTime(startTime).atZone(SEOUL).toInstant();
	}

	public static boolean isInPeriod(
			LocalDate weekStartMonday,
			DayOfWeek dayOfWeek,
			LocalTime startTime,
			Instant periodStart,
			Instant periodEnd) {
		Instant occurrence = occurrenceAt(weekStartMonday, dayOfWeek, startTime);
		return !occurrence.isBefore(periodStart) && !occurrence.isAfter(periodEnd);
	}
}
