package com.example.ams.api.dto;

import com.example.ams.domain.user.Subject;
import com.example.ams.domain.user.User;
import com.example.ams.domain.user.UserRole;
import com.example.ams.domain.user.UserStatus;

public record TokenResponse(
		String accessToken,
		String refreshToken,
		UserSummary user) {

	public static TokenResponse of(String accessToken, String refreshToken, User user) {
		return new TokenResponse(accessToken, refreshToken, UserSummary.from(user));
	}

	public record UserSummary(
			long userId,
			long academyId,
			String email,
			String name,
			UserRole role,
			Subject subject,
			UserStatus status) {

		public static UserSummary from(User user) {
			return new UserSummary(
					user.userId(),
					user.academyId(),
					user.email(),
					user.name(),
					user.role(),
					user.subject(),
					user.status());
		}
	}
}
