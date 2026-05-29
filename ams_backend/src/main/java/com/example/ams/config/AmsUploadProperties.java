package com.example.ams.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ams.upload")
public record AmsUploadProperties(String dir, int maxSizeMb) {

	public AmsUploadProperties {
		if (dir == null || dir.isBlank()) {
			dir = "uploads";
		}
		if (maxSizeMb <= 0) {
			maxSizeMb = 10;
		}
	}
}
