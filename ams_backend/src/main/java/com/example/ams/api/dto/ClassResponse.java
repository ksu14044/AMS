package com.example.ams.api.dto;

import java.time.Instant;

import com.example.ams.domain.clazz.Clazz;
import com.example.ams.domain.user.Subject;

public record ClassResponse(
		long classId,
		long academyId,
		Subject subject,
		String name,
		long homeroomTeacherId,
		String classroom,
		Instant createdAt) {

	public static ClassResponse from(Clazz clazz) {
		return new ClassResponse(
				clazz.classId(),
				clazz.academyId(),
				clazz.subject(),
				clazz.name(),
				clazz.homeroomTeacherId(),
				clazz.classroom(),
				clazz.createdAt());
	}
}
