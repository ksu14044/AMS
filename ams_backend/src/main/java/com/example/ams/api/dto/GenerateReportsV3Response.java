package com.example.ams.api.dto;

import java.time.Instant;
import java.util.List;

public record GenerateReportsV3Response(
		int created,
		Instant periodStart,
		Instant periodEnd,
		String periodLabel,
		List<Long> reportIds) {
}
