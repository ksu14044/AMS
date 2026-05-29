package com.example.ams.api.report;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

	@GetMapping("/{reportId}/pdf")
	public ResponseEntity<Resource> downloadPdf(@PathVariable long reportId) {
		Resource resource = diligenceReportService.loadPdfResource(reportId);
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"diligence-report-" + reportId + ".pdf\"")
				.contentType(MediaType.APPLICATION_PDF)
				.body(resource);
	}
}
