package com.example.ams.api.dto;

import java.time.Instant;

import com.example.ams.domain.auth.SignupInviteKind;
import com.example.ams.domain.user.UserRole;

public record SignupInviteResponse(
		SignupInviteKind kind,
		UserRole role,
		String roleLabel,
		String url,
		Instant expiresAt) {
}
