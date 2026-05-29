package com.example.ams.api.dto;

import com.example.ams.repository.AssistantClassAssignmentRepository.AssignedAssistant;

public record AssignedAssistantResponse(long assignmentId, long assistantId, String assistantName) {

	public static AssignedAssistantResponse from(AssignedAssistant row) {
		return new AssignedAssistantResponse(row.assignmentId(), row.assistantId(), row.assistantName());
	}
}
