package com.example.ams.api.dto;

import java.time.LocalDate;

public record LessonRecordLinkedItemResponse(
		String type,
		long id,
		String title,
		Integer questionCount,
		Integer retakeThresholdCount,
		String youtubeUrl,
		LocalDate clinicDate,
		String clinicStartTime,
		Long assistantId,
		Integer maxCapacity,
		AssignmentTargetResponse targets,
		boolean canDelete,
		boolean canEdit) {
}
