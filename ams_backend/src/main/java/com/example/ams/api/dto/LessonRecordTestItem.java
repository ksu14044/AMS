package com.example.ams.api.dto;

import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LessonRecordTestItem(
		@NotBlank @Size(max = 200) String title,
		@Min(1) Integer questionCount,
		@Min(1) Integer retakeThresholdCount,
		List<Long> targetStudentIds) {
}
