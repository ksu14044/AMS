package com.example.ams.api.dto;

import java.util.List;

import com.example.ams.service.AssistantAssignmentService.ClassAssistantsView;

public record ClassAssistantsResponse(
		List<AssignedAssistantResponse> assigned,
		List<AssistantOptionResponse> available) {

	public static ClassAssistantsResponse from(ClassAssistantsView view) {
		return new ClassAssistantsResponse(
				view.assigned().stream().map(AssignedAssistantResponse::from).toList(),
				view.available().stream().map(AssistantOptionResponse::from).toList());
	}
}
