package com.example.ams.api.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;

public record SaveAssignmentTargetRequest(@NotNull List<Long> studentIds) {
}
