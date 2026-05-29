package com.example.ams.api.dto;

import java.time.Instant;

import com.example.ams.domain.clazz.Textbook;

public record TextbookResponse(
		long classId,
		String title,
		String publisher,
		String progressNote,
		Instant updatedAt) {

	public static TextbookResponse from(Textbook textbook) {
		return new TextbookResponse(
				textbook.classId(),
				textbook.title(),
				textbook.publisher(),
				textbook.progressNote(),
				textbook.updatedAt());
	}

	public static TextbookResponse empty(long classId) {
		return new TextbookResponse(classId, null, null, null, null);
	}
}
