package com.example.ams.api.dto;

import com.example.ams.domain.study.GaugeLevel;

public record StudyRecordResponse(
		long classId,
		long studentId,
		String studentName,
		StudyRecordMetricResponse homework,
		StudyRecordMetricResponse clinic,
		StudyRecordTestMetricResponse test,
		StudyRecordMetricResponse video,
		int overallPercent,
		String overallGrade,
		GaugeLevel gaugeLevel,
		String encouragementMessage) {
}
