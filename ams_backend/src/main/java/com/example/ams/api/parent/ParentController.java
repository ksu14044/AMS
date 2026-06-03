package com.example.ams.api.parent;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ams.api.dto.DiligenceReportListResponse;
import com.example.ams.api.dto.ParentChildResponse;
import com.example.ams.api.dto.PendingTaskResponse;
import com.example.ams.api.dto.StudyRecordResponse;
import com.example.ams.common.ApiResponse;
import com.example.ams.service.ParentPortalService;

@RestController
@RequestMapping("/api/v1/parent")
@PreAuthorize("hasRole('PARENT')")
public class ParentController {

	private final ParentPortalService parentPortalService;

	public ParentController(ParentPortalService parentPortalService) {
		this.parentPortalService = parentPortalService;
	}

	@GetMapping("/children")
	public ApiResponse<List<ParentChildResponse>> listChildren() {
		return ApiResponse.ok(parentPortalService.listChildren());
	}

	@GetMapping("/children/{studentId}/pending-tasks")
	public ApiResponse<List<PendingTaskResponse>> pendingTasks(@PathVariable long studentId) {
		return ApiResponse.ok(parentPortalService.listPendingTasks(studentId));
	}

	@GetMapping("/children/{studentId}/classes/{classId}/study-records")
	public ApiResponse<StudyRecordResponse> studyRecord(
			@PathVariable long studentId,
			@PathVariable long classId) {
		return ApiResponse.ok(parentPortalService.getStudyRecord(studentId, classId));
	}

	@GetMapping("/children/{studentId}/reports")
	public ApiResponse<List<DiligenceReportListResponse>> reports(@PathVariable long studentId) {
		return ApiResponse.ok(parentPortalService.listReports(studentId));
	}
}
