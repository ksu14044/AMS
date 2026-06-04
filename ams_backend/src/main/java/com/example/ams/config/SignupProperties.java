package com.example.ams.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "ams.signup")
public record SignupProperties(
		@DefaultValue("http://localhost:5173") String frontendBaseUrl,
		/** @deprecated 12.10-6 — POST /auth/signup-invites/academy. 공개 가입은 POST /auth/signup/academy (inviteToken 없음) */
		String academyBootstrapKey) {
}
