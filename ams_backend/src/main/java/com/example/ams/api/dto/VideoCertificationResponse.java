package com.example.ams.api.dto;

import java.time.Instant;

import com.example.ams.domain.clazz.VideoCertification;

public record VideoCertificationResponse(
		long certificationId,
		long videoId,
		long studentId,
		String imageUrl,
		Instant submittedAt) {

	public static VideoCertificationResponse from(VideoCertification certification) {
		return new VideoCertificationResponse(
				certification.certificationId(),
				certification.videoId(),
				certification.studentId(),
				certification.imageUrl(),
				certification.submittedAt());
	}
}
