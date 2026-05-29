package com.example.ams.api.clazz;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ams.api.dto.ClassNoticeResponse;
import com.example.ams.api.dto.CreateClassNoticeRequest;
import com.example.ams.common.ApiResponse;
import com.example.ams.domain.clazz.ClassNotice;
import com.example.ams.service.ClassNoticeService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/classes/{classId}/notices")
public class ClassNoticeController {

	private final ClassNoticeService classNoticeService;

	public ClassNoticeController(ClassNoticeService classNoticeService) {
		this.classNoticeService = classNoticeService;
	}

	@GetMapping
	public ApiResponse<List<ClassNoticeResponse>> list(@PathVariable long classId) {
		List<ClassNoticeResponse> notices = classNoticeService.listNotices(classId).stream()
				.map(ClassNoticeResponse::from)
				.toList();
		return ApiResponse.ok(notices);
	}

	@PostMapping
	public ApiResponse<ClassNoticeResponse> create(
			@PathVariable long classId,
			@Valid @RequestBody CreateClassNoticeRequest request) {
		ClassNotice created = classNoticeService.createNotice(
				classId,
				request.title(),
				request.body(),
				request.attachmentUrl());
		return ApiResponse.ok(ClassNoticeResponse.from(created));
	}
}
