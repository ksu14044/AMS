package com.example.ams.domain.clazz;

import java.time.Instant;
import java.time.LocalDate;

public record LessonRecord(
		long lessonRecordId,
		long classId,
		LocalDate lessonDate,
		String summary,
		long authorId,
		Instant createdAt,
		Instant updatedAt) {
}
