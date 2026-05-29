package com.example.ams.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LoginSelectRequest(@NotBlank String loginToken, @NotNull Long userId) {
}
