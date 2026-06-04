package com.example.ams.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param academyBootstrapKey 12.10-6 이후 deprecated — bootstrap 초대 링크 발급용. 공개 학원 개설은 inviteToken 없이 {@code POST /auth/signup/academy}
 */
@ConfigurationProperties(prefix = "ams.signup")
public record SignupProperties(
		@DefaultValue("http://localhost:5173") String frontendBaseUrl,
		String academyBootstrapKey) {
}
