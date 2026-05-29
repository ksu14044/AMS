package com.example.ams.api.clazz;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ams.api.dto.TextbookResponse;
import com.example.ams.api.dto.UpdateTextbookRequest;
import com.example.ams.common.ApiResponse;
import com.example.ams.service.TextbookService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/classes/{classId}/textbook")
public class ClassTextbookController {

	private final TextbookService textbookService;

	public ClassTextbookController(TextbookService textbookService) {
		this.textbookService = textbookService;
	}

	@GetMapping
	public ApiResponse<TextbookResponse> getTextbook(@PathVariable long classId) {
		TextbookResponse response = textbookService.getTextbook(classId)
				.map(TextbookResponse::from)
				.orElse(TextbookResponse.empty(classId));
		return ApiResponse.ok(response);
	}

	@PatchMapping
	public ApiResponse<TextbookResponse> updateTextbook(
			@PathVariable long classId,
			@Valid @RequestBody UpdateTextbookRequest request) {
		TextbookResponse response = TextbookResponse.from(textbookService.updateTextbook(
				classId,
				request.title(),
				request.publisher(),
				request.progressNote()));
		return ApiResponse.ok(response);
	}
}
