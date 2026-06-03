package com.example.ams.api;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ams.api.dto.CreateParentLinkRequest;
import com.example.ams.api.dto.ParentLinkResponse;
import com.example.ams.common.ApiResponse;
import com.example.ams.service.ParentPortalService;
import com.example.ams.service.ParentStudentLinkService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/parent-links")
public class ParentLinkController {

	private final ParentStudentLinkService linkService;
	private final ParentPortalService parentPortalService;

	public ParentLinkController(ParentStudentLinkService linkService, ParentPortalService parentPortalService) {
		this.linkService = linkService;
		this.parentPortalService = parentPortalService;
	}

	@PostMapping
	@PreAuthorize("hasAnyRole('ACADEMY_ADMIN', 'STAFF_OFFICE', 'TEACHER_KO', 'TEACHER_EN', 'TEACHER_MATH', 'ASSISTANT_KO', 'ASSISTANT_EN', 'ASSISTANT_MATH')")
	public ApiResponse<ParentLinkResponse> create(@Valid @RequestBody CreateParentLinkRequest request) {
		return ApiResponse.ok(parentPortalService.createLink(request.parentEmail(), request.studentId()));
	}

	@DeleteMapping("/{linkId}")
	@PreAuthorize("hasAnyRole('ACADEMY_ADMIN', 'STAFF_OFFICE', 'TEACHER_KO', 'TEACHER_EN', 'TEACHER_MATH', 'ASSISTANT_KO', 'ASSISTANT_EN', 'ASSISTANT_MATH')")
	public ApiResponse<Void> delete(@PathVariable long linkId) {
		linkService.deleteLink(linkId);
		return ApiResponse.ok(null);
	}

	@GetMapping("/by-student/{studentId}")
	@PreAuthorize("hasAnyRole('ACADEMY_ADMIN', 'STAFF_OFFICE', 'TEACHER_KO', 'TEACHER_EN', 'TEACHER_MATH', 'ASSISTANT_KO', 'ASSISTANT_EN', 'ASSISTANT_MATH')")
	public ApiResponse<List<ParentLinkResponse>> listByStudent(@PathVariable long studentId) {
		return ApiResponse.ok(parentPortalService.listLinksForStudentAsStaff(studentId));
	}
}
