package com.example.ams.api.dto;

import com.example.ams.domain.auth.SignupInviteKind;
import com.example.ams.domain.user.UserRole;

import jakarta.validation.constraints.NotNull;

public record CreateSignupInviteRequest(
		@NotNull SignupInviteKind kind,
		UserRole role) {
}
