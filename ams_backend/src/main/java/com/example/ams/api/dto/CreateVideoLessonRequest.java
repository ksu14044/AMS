package com.example.ams.api.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateVideoLessonRequest(
		@NotBlank @Size(max = 500) String youtubeUrl,
		@NotBlank @Size(max = 200) String title,
		String description,
		Instant publishedAt) {
}
