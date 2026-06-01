package com.example.ams.api.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Positive;

public record CreateEnrollmentRequest(
		@Positive long studentId,
		LocalDate accessibleFrom) {
}
