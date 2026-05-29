package com.example.ams.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateClassNoticeRequest(
		@NotBlank @Size(max = 200) String title,
		@NotBlank String body,
		@Size(max = 500) String attachmentUrl) {
}
