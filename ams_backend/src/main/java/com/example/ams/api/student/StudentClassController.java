package com.example.ams.api.student;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ams.api.dto.ClassResponse;
import com.example.ams.common.ApiResponse;
import com.example.ams.service.StudentClassService;

@RestController
@RequestMapping("/api/v1/student")
@PreAuthorize("hasRole('STUDENT')")
public class StudentClassController {

	private final StudentClassService studentClassService;

	public StudentClassController(StudentClassService studentClassService) {
		this.studentClassService = studentClassService;
	}

	@GetMapping("/classes")
	public ApiResponse<List<ClassResponse>> listMyClasses() {
		List<ClassResponse> classes = studentClassService.listMyClasses().stream()
				.map(ClassResponse::from)
				.toList();
		return ApiResponse.ok(classes);
	}
}
