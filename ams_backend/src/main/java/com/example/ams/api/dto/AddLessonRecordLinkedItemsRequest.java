package com.example.ams.api.dto;

public record AddLessonRecordLinkedItemsRequest(
		LessonRecordHomeworkItem homework,
		LessonRecordTestItem test,
		LessonRecordVideoItem video,
		LessonRecordClinicItem clinic) {
}
