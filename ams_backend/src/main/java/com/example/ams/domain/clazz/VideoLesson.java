package com.example.ams.domain.clazz;

import java.time.Instant;

public record VideoLesson(
		long videoId,
		long classId,
		String youtubeUrl,
		String title,
		String description,
		String thumbnailUrl,
		Instant publishedAt,
		long authorId,
		Instant createdAt,
		Instant updatedAt) {
}
