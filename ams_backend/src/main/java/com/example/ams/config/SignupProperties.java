package com.example.ams.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "ams.signup")
public record SignupProperties(
		@DefaultValue("http://localhost:5173") String frontendBaseUrl,
		/** 학원 최초 개설용. 설정 시 POST /auth/signup-invites/academy (X-Ams-Bootstrap-Key) 로 ACADEMY 링크 발급 */
		String academyBootstrapKey) {
}
