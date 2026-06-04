package com.example.ams.api.clazz;

import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ams.api.dto.DiligenceReportListResponse;
import com.example.ams.api.dto.GenerateReportsRequest;
import com.example.ams.api.dto.GenerateReportsResponse;
import com.example.ams.api.dto.GenerateReportsV3Response;
import com.example.ams.api.dto.ReportGenerationTargetResponse;
import com.example.ams.api.dto.ReportPeriodArchiveRequest;
import com.example.ams.service.DiligenceReportService.GenerateReportsV3Result;

import jakarta.validation.Valid;
import com.example.ams.common.ApiResponse;
import com.example.ams.service.DiligenceReportService;

@RestController
@RequestMapping("/api/v1/classes/{classId}/reports")
public class ClassReportController {

	private final DiligenceReportService diligenceReportService;

	public ClassReportController(DiligenceReportService diligenceReportService) {
		this.diligenceReportService = diligenceReportService;
	}

	@GetMapping
	public ApiResponse<List<DiligenceReportListResponse>> list(@PathVariable long classId) {
		List<DiligenceReportListResponse> list = diligenceReportService.listByClass(classId).stream()
				.map(DiligenceReportListResponse::from)
				.toList();
		return ApiResponse.ok(list);
	}

	@GetMapping("/generation-targets")
	public ApiResponse<List<ReportGenerationTargetResponse>> listGenerationTargets(@PathVariable long classId) {
		List<ReportGenerationTargetResponse> list = diligenceReportService.listGenerationTargets(classId).stream()
				.map(ReportGenerationTargetResponse::from)
				.toList();
		return ApiResponse.ok(list);
	}

	@PostMapping("/generate")
	public ApiResponse<GenerateReportsV3Response> generate(
			@PathVariable long classId,
			@Valid @RequestBody GenerateReportsRequest request) {
		GenerateReportsV3Result result = diligenceReportService.generateReportsForPeriod(classId, request);
		return ApiResponse.ok(new GenerateReportsV3Response(
				result.created(),
				result.periodStart(),
				result.periodEnd(),
				result.periodLabel(),
				result.reportIds()));
	}

	/** @deprecated v3.0 — {@link #generate(long, GenerateReportsRequest)} 사용 */
	@PostMapping("/generate/{testId}")
	public ApiResponse<GenerateReportsResponse> generateByTest(
			@PathVariable long classId,
			@PathVariable long testId) {
		int created = diligenceReportService.generateReportsForTest(classId, testId);
		return ApiResponse.ok(new GenerateReportsResponse(testId, created));
	}

	@PostMapping("/archive")
	public ResponseEntity<Resource> downloadPeriodPdfArchive(
			@PathVariable long classId,
			@Valid @RequestBody ReportPeriodArchiveRequest request) {
		var archive = diligenceReportService.loadPeriodPdfArchive(
				classId,
				request.periodStart(),
				request.periodEnd());
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + archive.filename() + "\"")
				.contentType(MediaType.parseMediaType("application/zip"))
				.body(archive.resource());
	}

	@GetMapping("/tests/{testId}/pdf-archive")
	public ResponseEntity<Resource> downloadTestPdfArchive(
			@PathVariable long classId,
			@PathVariable long testId) {
		var archive = diligenceReportService.loadTestPdfArchive(classId, testId);
		String asciiFilename = "reports-test-" + testId + ".zip";
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + asciiFilename + "\"")
				.contentType(MediaType.parseMediaType("application/zip"))
				.body(archive.resource());
	}
}
