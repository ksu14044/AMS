package com.example.ams.api.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotNull;

public record CreateTestRetakeRequest(@NotNull Instant testAt) {
}
