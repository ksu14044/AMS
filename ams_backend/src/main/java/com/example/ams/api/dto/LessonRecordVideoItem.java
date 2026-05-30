package com.example.ams.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LessonRecordVideoItem(
		@NotBlank @Size(max = 200) String title,
		@NotBlank String youtubeUrl) {
}
