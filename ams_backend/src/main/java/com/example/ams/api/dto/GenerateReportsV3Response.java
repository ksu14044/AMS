package com.example.ams.api.dto;

import java.time.Instant;

public record GenerateReportsV3Response(
		int created,
		Instant periodStart,
		Instant periodEnd,
		String periodLabel) {
}
