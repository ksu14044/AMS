package com.example.ams.api.dto;

import com.example.ams.domain.user.Subject;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateClassRequest(
		@NotNull Subject subject,
		@NotBlank @Size(max = 100) String name,
		@Positive long homeroomTeacherId,
		@Size(max = 50) String classroom) {
}
