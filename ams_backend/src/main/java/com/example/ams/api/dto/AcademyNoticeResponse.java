package com.example.ams.api.dto;

import java.time.Instant;

import com.example.ams.domain.academy.AcademyNotice;

public record AcademyNoticeResponse(
		long noticeId,
		long academyId,
		String title,
		String body,
		String attachmentUrl,
		Instant publishedAt,
		long authorId) {

	public static AcademyNoticeResponse from(AcademyNotice notice) {
		return new AcademyNoticeResponse(
				notice.noticeId(),
				notice.academyId(),
				notice.title(),
				notice.body(),
				notice.attachmentUrl(),
				notice.publishedAt(),
				notice.authorId());
	}
}
