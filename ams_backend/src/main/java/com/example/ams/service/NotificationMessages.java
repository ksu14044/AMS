package com.example.ams.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import com.example.ams.domain.clazz.Clazz;
import com.example.ams.domain.clazz.DayOfWeek;
import com.example.ams.domain.user.Subject;

final class NotificationMessages {

	static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

	private NotificationMessages() {
	}

	static String classLabel(Subject subject, String className) {
		return "[" + subjectLabel(subject) + " " + className + "]";
	}

	static String subjectLabel(Subject subject) {
		return switch (subject) {
			case KO -> "국어";
			case EN -> "영어";
			case MATH -> "수학";
		};
	}

	static String clinicSlotLabel(DayOfWeek dayOfWeek, LocalTime startTime) {
		return dayLabel(dayOfWeek) + " " + formatTime(startTime);
	}

	static String dayLabel(DayOfWeek dayOfWeek) {
		return switch (dayOfWeek) {
			case MON -> "월";
			case TUE -> "화";
			case WED -> "수";
			case THU -> "목";
			case FRI -> "금";
			case SAT -> "토";
			case SUN -> "일";
		};
	}

	static String formatTime(LocalTime time) {
		return String.format("%d:%02d", time.getHour(), time.getMinute());
	}

	static ZonedDateTime seoulDayStart(LocalDate date) {
		return date.atStartOfDay(SEOUL);
	}
}
