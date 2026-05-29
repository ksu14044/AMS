package com.example.ams.domain.clazz;

import java.time.Instant;

public record Textbook(
		long classId,
		String title,
		String publisher,
		String progressNote,
		Instant updatedAt) {
}
