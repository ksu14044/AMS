package com.example.ams.api.student;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.ams.api.dto.StudentRosterItemResponse;
import com.example.ams.common.ApiResponse;
import com.example.ams.domain.user.StudentRosterRow;
import com.example.ams.service.StudentRosterService;

@RestController
@RequestMapping("/api/v1/students")
public class StudentRosterController {

	private final StudentRosterService studentRosterService;

	public StudentRosterController(StudentRosterService studentRosterService) {
		this.studentRosterService = studentRosterService;
	}

	@GetMapping("/roster")
	@PreAuthorize("hasAnyRole('ACADEMY_ADMIN', 'STAFF_OFFICE', 'TEACHER_KO', 'TEACHER_EN', 'TEACHER_MATH')")
	public ApiResponse<List<StudentRosterItemResponse>> roster(@RequestParam(required = false) String q) {
		List<StudentRosterRow> rows = studentRosterService.listRoster(q);
		return ApiResponse.ok(rows.stream().map(StudentRosterItemResponse::from).toList());
	}
}
