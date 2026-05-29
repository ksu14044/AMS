package com.example.ams.service;

public record StudyRecordPeriodMetrics(
		int homeworkSubmitted,
		int homeworkTotal,
		Integer homeworkRate,
		int clinicAttended,
		int clinicTotal,
		Integer clinicRate,
		int videoCertified,
		int videoTotal,
		Integer videoRate) {

	public static StudyRecordPeriodMetrics empty() {
		return new StudyRecordPeriodMetrics(0, 0, null, 0, 0, null, 0, 0, null);
	}
}
