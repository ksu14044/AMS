package com.example.ams.api.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateLessonRecordRequest(@NotBlank String summary) {
}
