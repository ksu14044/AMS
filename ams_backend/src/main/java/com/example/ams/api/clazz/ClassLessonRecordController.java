package com.example.ams.api.clazz;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ams.api.dto.CreateLessonRecordRequest;
import com.example.ams.api.dto.LessonRecordResponse;
import com.example.ams.api.dto.UpdateLessonRecordRequest;
import com.example.ams.common.ApiResponse;
import com.example.ams.service.LessonRecordService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/classes/{classId}/lesson-records")
public class ClassLessonRecordController {

	private final LessonRecordService lessonRecordService;

	public ClassLessonRecordController(LessonRecordService lessonRecordService) {
		this.lessonRecordService = lessonRecordService;
	}

	@GetMapping
	public ApiResponse<List<LessonRecordResponse>> list(@PathVariable long classId) {
		List<LessonRecordResponse> rows = lessonRecordService.listLessonRecords(classId).stream()
				.map(LessonRecordResponse::from)
				.toList();
		return ApiResponse.ok(rows);
	}

	@GetMapping("/{lessonRecordId}")
	public ApiResponse<LessonRecordResponse> get(
			@PathVariable long classId,
			@PathVariable long lessonRecordId) {
		var row = lessonRecordService.getLessonRecord(lessonRecordId);
		assertSameClass(classId, row.record().classId());
		return ApiResponse.ok(LessonRecordResponse.from(row));
	}

	@PostMapping
	public ApiResponse<LessonRecordResponse> create(
			@PathVariable long classId,
			@Valid @RequestBody CreateLessonRecordRequest request) {
		var row = lessonRecordService.createLessonRecord(classId, request);
		return ApiResponse.ok(LessonRecordResponse.from(row));
	}

	@PatchMapping("/{lessonRecordId}")
	public ApiResponse<LessonRecordResponse> update(
			@PathVariable long classId,
			@PathVariable long lessonRecordId,
			@Valid @RequestBody UpdateLessonRecordRequest request) {
		var existing = lessonRecordService.getLessonRecord(lessonRecordId);
		assertSameClass(classId, existing.record().classId());
		var row = lessonRecordService.updateLessonRecord(lessonRecordId, request.summary());
		return ApiResponse.ok(LessonRecordResponse.from(row));
	}

	private static void assertSameClass(long classId, long recordClassId) {
		if (classId != recordClassId) {
			throw new com.example.ams.common.BusinessException(
					com.example.ams.common.ErrorCode.LESSON_RECORD_NOT_FOUND);
		}
	}
}
