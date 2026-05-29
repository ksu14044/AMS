package com.example.ams.domain.academy;

import java.time.Instant;

public record AcademyNotice(
		long noticeId,
		long academyId,
		String title,
		String body,
		String attachmentUrl,
		Instant publishedAt,
		long authorId,
		Instant createdAt,
		Instant updatedAt) {
}
