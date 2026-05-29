package com.example.ams.api.dto;

import jakarta.validation.constraints.Positive;

public record CreateEnrollmentRequest(@Positive long studentId) {
}
