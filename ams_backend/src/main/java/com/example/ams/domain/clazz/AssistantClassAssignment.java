package com.example.ams.domain.clazz;

import java.time.Instant;

public record AssistantClassAssignment(
		long assignmentId,
		long assistantId,
		long classId,
		long assignedBy,
		Instant assignedAt) {
}
