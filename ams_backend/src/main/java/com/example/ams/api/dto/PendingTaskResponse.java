package com.example.ams.api.dto;

import com.example.ams.domain.pending.PendingTaskType;
import com.example.ams.service.PendingTaskService.PendingTaskItem;

public record PendingTaskResponse(
		PendingTaskType type,
		long classId,
		String className,
		String subject,
		long entityId,
		String title) {

	public static PendingTaskResponse from(PendingTaskItem item) {
		return new PendingTaskResponse(
				item.type(),
				item.classId(),
				item.className(),
				item.subject().name(),
				item.entityId(),
				item.title());
	}
}
