package com.example.ams.api.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record GradeHomeworkSubmissionRequest(@NotNull List<@Size(max = 500) String> answers) {
}
