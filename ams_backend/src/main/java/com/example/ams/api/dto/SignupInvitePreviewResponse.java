package com.example.ams.api.dto;

import java.time.Instant;

import com.example.ams.domain.auth.SignupInviteKind;
import com.example.ams.domain.auth.SignupInvitePayload;
import com.example.ams.domain.user.Subject;
import com.example.ams.domain.user.UserRole;

public record SignupInvitePreviewResponse(
		SignupInviteKind kind,
		String academyCode,
		String academyName,
		UserRole role,
		String roleLabel,
		Subject subject,
		Instant expiresAt) {

	public static SignupInvitePreviewResponse of(
			SignupInvitePayload invite,
			String academyName,
			String roleLabel) {
		return new SignupInvitePreviewResponse(
				invite.kind(),
				invite.academyCode(),
				academyName,
				invite.role(),
				roleLabel,
				invite.subject(),
				invite.expiresAt());
	}
}
