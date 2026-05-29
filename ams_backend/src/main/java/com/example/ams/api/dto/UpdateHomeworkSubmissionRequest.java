package com.example.ams.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.validation.constraints.Size;

public record UpdateHomeworkSubmissionRequest(
		boolean submitted,
		Instant submittedAt,
		BigDecimal score,
		@Size(max = 16) String grade,
		@Size(max = 500) String memo) {
}
