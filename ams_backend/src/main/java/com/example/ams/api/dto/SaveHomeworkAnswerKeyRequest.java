package com.example.ams.api.dto;

import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SaveHomeworkAnswerKeyRequest(
		@NotNull @Min(1) Integer questionCount,
		@NotEmpty List<@Size(max = 500) String> answers) {
}
