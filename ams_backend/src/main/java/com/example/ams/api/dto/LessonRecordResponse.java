package com.example.ams.api.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.example.ams.domain.clazz.LessonRecord;
import com.example.ams.service.LessonRecordService.LessonRecordCounts;
import com.example.ams.service.LessonRecordService.LessonRecordRow;

public record LessonRecordResponse(
		long lessonRecordId,
		long classId,
		LocalDate lessonDate,
		String summary,
		long authorId,
		String authorName,
		int homeworkCount,
		int testCount,
		int videoCount,
		int clinicCount,
		List<LessonRecordLinkedItemResponse> linkedItems,
		Instant createdAt,
		Instant updatedAt) {

	public static LessonRecordResponse from(LessonRecordRow row) {
		LessonRecord record = row.record();
		LessonRecordCounts counts = row.counts();
		return new LessonRecordResponse(
				record.lessonRecordId(),
				record.classId(),
				record.lessonDate(),
				record.summary(),
				record.authorId(),
				row.authorName(),
				counts.homeworkCount(),
				counts.testCount(),
				counts.videoCount(),
				counts.clinicCount(),
				row.linkedItems(),
				record.createdAt(),
				record.updatedAt());
	}
}
