package com.example.ams.api.clazz;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ams.api.dto.ClassAssistantsResponse;
import com.example.ams.api.dto.UpdateClassAssistantsRequest;
import com.example.ams.common.ApiResponse;
import com.example.ams.service.AssistantAssignmentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/classes/{classId}/assistants")
public class ClassAssistantController {

	private final AssistantAssignmentService assistantAssignmentService;

	public ClassAssistantController(AssistantAssignmentService assistantAssignmentService) {
		this.assistantAssignmentService = assistantAssignmentService;
	}

	@GetMapping
	public ApiResponse<ClassAssistantsResponse> getAssistants(@PathVariable long classId) {
		return ApiResponse.ok(
				ClassAssistantsResponse.from(assistantAssignmentService.getAssistantsForClass(classId)));
	}

	@PutMapping
	public ApiResponse<ClassAssistantsResponse> replaceAssistants(
			@PathVariable long classId,
			@Valid @RequestBody UpdateClassAssistantsRequest request) {
		return ApiResponse.ok(ClassAssistantsResponse.from(
				assistantAssignmentService.replaceAssignments(classId, request.assistantIds())));
	}
}
