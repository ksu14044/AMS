package com.example.ams.api.dto;

import java.time.Instant;

import com.example.ams.domain.user.Subject;
import com.example.ams.domain.user.User;
import com.example.ams.domain.user.UserRole;
import com.example.ams.domain.user.UserStatus;

public record UserResponse(
		long userId,
		long academyId,
		String email,
		String name,
		UserRole role,
		Subject subject,
		UserStatus status,
		Instant createdAt) {

	public static UserResponse from(User user) {
		return new UserResponse(
				user.userId(),
				user.academyId(),
				user.email(),
				user.name(),
				user.role(),
				user.subject(),
				user.status(),
				user.createdAt());
	}
}
