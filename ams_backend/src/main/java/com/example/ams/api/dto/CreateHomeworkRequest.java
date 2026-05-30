package com.example.ams.api.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateHomeworkRequest(
		@NotBlank @Size(max = 200) String title,
		Integer questionCount,
		List<Long> targetStudentIds) {
}
