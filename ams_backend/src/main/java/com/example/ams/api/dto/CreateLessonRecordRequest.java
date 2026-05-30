package com.example.ams.api.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateLessonRecordRequest(
		@NotNull LocalDate lessonDate,
		@NotBlank String summary,
		LessonRecordHomeworkItem homework,
		LessonRecordTestItem test,
		LessonRecordVideoItem video,
		LessonRecordClinicItem clinic) {
}
