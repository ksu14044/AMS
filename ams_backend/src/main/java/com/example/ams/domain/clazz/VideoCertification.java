package com.example.ams.domain.clazz;

import java.time.Instant;

public record VideoCertification(
		long certificationId,
		long videoId,
		long studentId,
		String imageUrl,
		Instant submittedAt) {
}
