package com.example.ams.api.video;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.ams.api.dto.VideoCertificationResponse;
import com.example.ams.common.ApiResponse;
import com.example.ams.domain.clazz.VideoCertification;
import com.example.ams.service.VideoCertificationService;

@RestController
@RequestMapping("/api/v1/videos/{videoId}/certifications")
public class VideoCertificationController {

	private final VideoCertificationService videoCertificationService;

	public VideoCertificationController(VideoCertificationService videoCertificationService) {
		this.videoCertificationService = videoCertificationService;
	}

	@GetMapping("/me")
	public ApiResponse<VideoCertificationResponse> myCertification(@PathVariable long videoId) {
		VideoCertification certification = videoCertificationService.getMyCertification(videoId);
		if (certification == null) {
			return ApiResponse.ok(null);
		}
		return ApiResponse.ok(VideoCertificationResponse.from(certification));
	}

	@GetMapping
	public ApiResponse<List<VideoCertificationResponse>> list(@PathVariable long videoId) {
		List<VideoCertificationResponse> list = videoCertificationService.listCertifications(videoId).stream()
				.map(VideoCertificationResponse::from)
				.toList();
		return ApiResponse.ok(list);
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse<VideoCertificationResponse> submit(
			@PathVariable long videoId,
			@RequestPart("file") MultipartFile file) {
		VideoCertification created = videoCertificationService.submitCertification(videoId, file);
		return ApiResponse.ok(VideoCertificationResponse.from(created));
	}
}
