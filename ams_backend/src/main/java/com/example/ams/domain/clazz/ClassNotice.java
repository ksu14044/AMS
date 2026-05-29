package com.example.ams.domain.clazz;

import java.time.Instant;

public record ClassNotice(
		long noticeId,
		long classId,
		String title,
		String body,
		String attachmentUrl,
		Instant publishedAt,
		Instant scheduledAt,
		long authorId,
		Instant createdAt,
		Instant updatedAt) {
}
