package com.example.ams.api.report;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.ams.api.dto.DiligenceReportDetailResponse;
import com.example.ams.api.dto.UpdateReportCommentRequest;
import com.example.ams.common.ApiResponse;
import com.example.ams.service.DiligenceReportService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

	private final DiligenceReportService diligenceReportService;

	public ReportController(DiligenceReportService diligenceReportService) {
		this.diligenceReportService = diligenceReportService;
	}

	@GetMapping("/{reportId}")
	public ApiResponse<DiligenceReportDetailResponse> get(@PathVariable long reportId) {
		return ApiResponse.ok(DiligenceReportDetailResponse.from(diligenceReportService.getReport(reportId)));
	}

	@PatchMapping("/{reportId}/comment")
	public ApiResponse<DiligenceReportDetailResponse> updateComment(
			@PathVariable long reportId,
			@Valid @RequestBody UpdateReportCommentRequest request) {
		return ApiResponse.ok(DiligenceReportDetailResponse.from(
				diligenceReportService.updateComment(reportId, request.comment())));
	}

	@GetMapping("/{reportId}/image")
	public ResponseEntity<Resource> downloadImage(@PathVariable long reportId) {
		return downloadReportFile(reportId, true);
	}

	/** 구 클라이언트 호환: PNG(또는 기존 PDF) 파일 제공 */
	@GetMapping("/{reportId}/pdf")
	public ResponseEntity<Resource> downloadPdfLegacy(@PathVariable long reportId) {
		return downloadReportFile(reportId, false);
	}

	private ResponseEntity<Resource> downloadReportFile(long reportId, boolean inline) {
		var file = diligenceReportService.loadReportFile(reportId);
		String disposition = (inline ? "inline" : "attachment") + "; filename=\"" + file.downloadFilename() + "\"";
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, disposition)
				.contentType(file.mediaType())
				.body(file.resource());
	}

	@PostMapping(value = "/{reportId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse<Void> uploadImage(
			@PathVariable long reportId,
			@RequestPart("file") MultipartFile file) {
		diligenceReportService.storeReportImage(reportId, file);
		return ApiResponse.ok(null);
	}
}
