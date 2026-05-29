package com.example.ams.api.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;

public record UpdateClassAssistantsRequest(@NotNull List<Long> assistantIds) {
}
