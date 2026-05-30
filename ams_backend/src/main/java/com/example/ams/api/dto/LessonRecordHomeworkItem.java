package com.example.ams.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LessonRecordHomeworkItem(
		@NotBlank @Size(max = 200) String title,
		@Min(1) Integer questionCount) {
}
