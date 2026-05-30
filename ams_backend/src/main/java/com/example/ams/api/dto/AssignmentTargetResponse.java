package com.example.ams.api.dto;

import java.util.List;

import com.example.ams.service.AssignmentTargetService.TargetView;

public record AssignmentTargetResponse(List<Long> targetStudentIds, boolean allClassTargeted) {

	public static AssignmentTargetResponse from(TargetView view) {
		return new AssignmentTargetResponse(view.targetStudentIds(), view.allClassTargeted());
	}
}
