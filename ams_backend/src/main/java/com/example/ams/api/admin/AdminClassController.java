package com.example.ams.api.admin;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ams.api.dto.ClassResponse;
import com.example.ams.api.dto.CreateClassRequest;
import com.example.ams.api.dto.UpdateClassRequest;
import com.example.ams.api.dto.CreateEnrollmentRequest;
import com.example.ams.api.dto.EnrollmentResponse;
import com.example.ams.api.dto.UserResponse;
import com.example.ams.common.ApiResponse;
import com.example.ams.domain.clazz.ClassEnrollment;
import com.example.ams.domain.clazz.Clazz;
import com.example.ams.service.AdminClassService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ACADEMY_ADMIN')")
public class AdminClassController {

	private final AdminClassService adminClassService;

	public AdminClassController(AdminClassService adminClassService) {
		this.adminClassService = adminClassService;
	}

	@GetMapping("/classes")
	public ApiResponse<List<ClassResponse>> listClasses() {
		List<ClassResponse> classes = adminClassService.listClasses().stream()
				.map(ClassResponse::from)
				.toList();
		return ApiResponse.ok(classes);
	}

	@PostMapping("/classes")
	public ApiResponse<ClassResponse> createClass(@Valid @RequestBody CreateClassRequest request) {
		Clazz created = adminClassService.createClass(
				request.subject(),
				request.name(),
				request.homeroomTeacherId(),
				request.classroom());
		return ApiResponse.ok(ClassResponse.from(created));
	}

	@PatchMapping("/classes/{classId}")
	public ApiResponse<ClassResponse> updateClass(
			@PathVariable long classId,
			@Valid @RequestBody UpdateClassRequest request) {
		Clazz updated = adminClassService.updateClass(
				classId,
				request.subject(),
				request.name(),
				request.homeroomTeacherId(),
				request.classroom());
		return ApiResponse.ok(ClassResponse.from(updated));
	}

	@GetMapping("/classes/{classId}/enrollments")
	public ApiResponse<List<EnrollmentResponse>> listEnrollments(@PathVariable long classId) {
		List<EnrollmentResponse> enrollments = adminClassService.listEnrollments(classId).stream()
				.map(EnrollmentResponse::from)
				.toList();
		return ApiResponse.ok(enrollments);
	}

	@PostMapping("/classes/{classId}/enrollments")
	public ApiResponse<EnrollmentResponse> enroll(
			@PathVariable long classId,
			@Valid @RequestBody CreateEnrollmentRequest request) {
		ClassEnrollment enrollment = adminClassService.enrollStudent(classId, request.studentId(), request.accessibleFrom());
		return ApiResponse.ok(EnrollmentResponse.from(enrollment));
	}

	@GetMapping("/teachers")
	public ApiResponse<List<UserResponse>> listTeachers() {
		List<UserResponse> teachers = adminClassService.listHomeroomTeachers().stream()
				.map(UserResponse::from)
				.toList();
		return ApiResponse.ok(teachers);
	}

	@GetMapping("/students")
	public ApiResponse<List<UserResponse>> listStudents() {
		List<UserResponse> students = adminClassService.listActiveStudents().stream()
				.map(UserResponse::from)
				.toList();
		return ApiResponse.ok(students);
	}

	@DeleteMapping("/enrollments/{enrollmentId}")
	public ApiResponse<Void> unenroll(@PathVariable long enrollmentId) {
		adminClassService.unenroll(enrollmentId);
		return ApiResponse.ok(null);
	}
}
