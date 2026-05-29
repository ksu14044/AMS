package com.example.ams.api.dto;

import com.example.ams.domain.clazz.Clazz;

public record ClassDetailResponse(
		long classId,
		long academyId,
		String subject,
		String name,
		long homeroomTeacherId,
		String classroom,
		boolean canManageContent) {

	public static ClassDetailResponse from(Clazz clazz, boolean canManageContent) {
		return new ClassDetailResponse(
				clazz.classId(),
				clazz.academyId(),
				clazz.subject().name(),
				clazz.name(),
				clazz.homeroomTeacherId(),
				clazz.classroom(),
				canManageContent);
	}
}
