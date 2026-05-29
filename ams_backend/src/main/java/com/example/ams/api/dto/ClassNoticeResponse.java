package com.example.ams.api.dto;

import java.time.Instant;

import com.example.ams.domain.clazz.ClassNotice;

public record ClassNoticeResponse(
		long noticeId,
		long classId,
		String title,
		String body,
		String attachmentUrl,
		Instant publishedAt,
		Instant scheduledAt,
		long authorId) {

	public static ClassNoticeResponse from(ClassNotice notice) {
		return new ClassNoticeResponse(
				notice.noticeId(),
				notice.classId(),
				notice.title(),
				notice.body(),
				notice.attachmentUrl(),
				notice.publishedAt(),
				notice.scheduledAt(),
				notice.authorId());
	}
}
