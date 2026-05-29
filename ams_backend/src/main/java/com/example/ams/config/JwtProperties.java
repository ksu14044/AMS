package com.example.ams.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "ams.jwt")
public record JwtProperties(
		String secret,
		long accessExpirationMs,
		long refreshExpirationMs,
		@DefaultValue("300000") long loginSelectExpirationMs,
		@DefaultValue("604800000") long signupInviteExpirationMs) {
}
