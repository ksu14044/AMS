package com.example.ams.api.dto;

import jakarta.validation.constraints.Size;

public record UpdateReportCommentRequest(@Size(max = 2000) String comment) {
}
