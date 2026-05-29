package com.example.ams.common;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import com.example.ams.domain.clazz.ClinicWeekStatus;

public final class ClinicBookingPolicy {

	public static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

	private ClinicBookingPolicy() {
	}

	/** 월요일 weekStart 기준, 토 23:00(서울) 전까지 예약·변경 가능 */
	public static boolean isWithinBookingWindow(LocalDate weekStart) {
		LocalDate saturday = weekStart.plusDays(5);
		ZonedDateTime deadline = saturday.atTime(23, 0).atZone(SEOUL);
		return ZonedDateTime.now(SEOUL).isBefore(deadline);
	}

	public static boolean canStudentBook(ClinicWeekStatus weekStatus, LocalDate weekStart) {
		return weekStatus == ClinicWeekStatus.OPEN && isWithinBookingWindow(weekStart);
	}
}
