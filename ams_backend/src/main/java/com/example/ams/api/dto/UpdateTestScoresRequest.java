package com.example.ams.api.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateTestScoresRequest(@NotNull @Valid List<StudentScoreInput> scores) {

	public record StudentScoreInput(
			@NotNull Long studentId,
			BigDecimal rawScore,
			@Size(max = 16) String grade) {
	}
}
