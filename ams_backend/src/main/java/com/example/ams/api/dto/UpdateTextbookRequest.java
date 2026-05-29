package com.example.ams.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateTextbookRequest(
		@NotBlank @Size(max = 200) String title,
		@Size(max = 100) String publisher,
		String progressNote) {
}
