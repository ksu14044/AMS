package com.example.ams.api.clazz;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ams.api.dto.CreateReportPeriodPresetRequest;
import com.example.ams.api.dto.ReportPeriodPresetResponse;
import com.example.ams.api.dto.UpdateReportPeriodPresetRequest;
import com.example.ams.common.ApiResponse;
import com.example.ams.service.ReportPeriodPresetService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/classes/{classId}/report-period-presets")
public class ClassReportPeriodPresetController {

	private final ReportPeriodPresetService presetService;

	public ClassReportPeriodPresetController(ReportPeriodPresetService presetService) {
		this.presetService = presetService;
	}

	@GetMapping
	public ApiResponse<List<ReportPeriodPresetResponse>> list(@PathVariable long classId) {
		List<ReportPeriodPresetResponse> presets = presetService.listPresets(classId).stream()
				.map(ReportPeriodPresetResponse::from)
				.toList();
		return ApiResponse.ok(presets);
	}

	@PostMapping
	public ApiResponse<ReportPeriodPresetResponse> create(
			@PathVariable long classId,
			@Valid @RequestBody CreateReportPeriodPresetRequest request) {
		return ApiResponse.ok(ReportPeriodPresetResponse.from(
				presetService.createPreset(classId, request.name(), request.periodStart(), request.periodEnd())));
	}

	@PutMapping("/{presetId}")
	public ApiResponse<ReportPeriodPresetResponse> update(
			@PathVariable long classId,
			@PathVariable long presetId,
			@Valid @RequestBody UpdateReportPeriodPresetRequest request) {
		return ApiResponse.ok(ReportPeriodPresetResponse.from(
				presetService.updatePreset(
						classId,
						presetId,
						request.name(),
						request.periodStart(),
						request.periodEnd())));
	}

	@DeleteMapping("/{presetId}")
	public ApiResponse<Void> delete(@PathVariable long classId, @PathVariable long presetId) {
		presetService.deletePreset(classId, presetId);
		return ApiResponse.ok(null);
	}
}
