package com.example.ams.api.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTestRequest(
		@NotBlank @Size(max = 200) String title,
		@NotNull Instant testAt) {
}
