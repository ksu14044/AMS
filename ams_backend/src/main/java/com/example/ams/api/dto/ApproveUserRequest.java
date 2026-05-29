package com.example.ams.api.dto;

import com.example.ams.domain.user.Subject;
import com.example.ams.domain.user.UserRole;

import jakarta.validation.constraints.NotNull;

public record ApproveUserRequest(@NotNull UserRole role, Subject subject) {
}
