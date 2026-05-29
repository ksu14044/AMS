package com.example.ams.api.clazz;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ams.api.dto.StudyRecordResponse;
import com.example.ams.api.dto.StudyRecordStudentOptionResponse;
import com.example.ams.common.ApiResponse;
import com.example.ams.service.StudyRecordService;

@RestController
@RequestMapping("/api/v1/classes/{classId}/study-records")
public class ClassStudyRecordController {

	private final StudyRecordService studyRecordService;

	public ClassStudyRecordController(StudyRecordService studyRecordService) {
		this.studyRecordService = studyRecordService;
	}

	@GetMapping("/me")
	public ApiResponse<StudyRecordResponse> myRecord(@PathVariable long classId) {
		return ApiResponse.ok(studyRecordService.getMyRecord(classId));
	}

	@GetMapping("/students")
	public ApiResponse<List<StudyRecordStudentOptionResponse>> listStudents(@PathVariable long classId) {
		return ApiResponse.ok(studyRecordService.listStudentOptions(classId));
	}

	@GetMapping("/students/{studentId}")
	public ApiResponse<StudyRecordResponse> studentRecord(
			@PathVariable long classId,
			@PathVariable long studentId) {
		return ApiResponse.ok(studyRecordService.getStudentRecord(classId, studentId));
	}
}
