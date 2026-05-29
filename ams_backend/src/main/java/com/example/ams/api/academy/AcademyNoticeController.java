package com.example.ams.api.academy;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ams.api.dto.AcademyNoticeResponse;
import com.example.ams.api.dto.CreateAcademyNoticeRequest;
import com.example.ams.common.ApiResponse;
import com.example.ams.domain.academy.AcademyNotice;
import com.example.ams.service.AcademyNoticeService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/academy/notices")
public class AcademyNoticeController {

	private final AcademyNoticeService academyNoticeService;

	public AcademyNoticeController(AcademyNoticeService academyNoticeService) {
		this.academyNoticeService = academyNoticeService;
	}

	@GetMapping
	public ApiResponse<List<AcademyNoticeResponse>> list() {
		List<AcademyNoticeResponse> notices = academyNoticeService.listNotices().stream()
				.map(AcademyNoticeResponse::from)
				.toList();
		return ApiResponse.ok(notices);
	}

	@PostMapping
	public ApiResponse<AcademyNoticeResponse> create(@Valid @RequestBody CreateAcademyNoticeRequest request) {
		AcademyNotice created = academyNoticeService.createNotice(
				request.title(),
				request.body(),
				request.attachmentUrl());
		return ApiResponse.ok(AcademyNoticeResponse.from(created));
	}
}
