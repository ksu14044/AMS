package com.example.ams.api.dto;

import com.example.ams.domain.user.UserRole;
import com.example.ams.service.AssistantAssignmentService.AssistantOption;

public record AssistantOptionResponse(long assistantId, String name, UserRole role) {

	public static AssistantOptionResponse from(AssistantOption option) {
		return new AssistantOptionResponse(option.assistantId(), option.name(), option.role());
	}
}
