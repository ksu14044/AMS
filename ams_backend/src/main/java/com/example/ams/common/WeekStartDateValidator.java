package com.example.ams.common;

import java.time.DayOfWeek;
import java.time.LocalDate;

public final class WeekStartDateValidator {

	private WeekStartDateValidator() {
	}

	public static LocalDate requireMonday(LocalDate date) {
		if (date == null) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "주차 시작일(월요일)이 필요합니다.");
		}
		if (date.getDayOfWeek() != DayOfWeek.MONDAY) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "주차 시작일은 월요일이어야 합니다.");
		}
		return date;
	}
}
