package com.example.ams.api.clazz;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ams.api.dto.ClinicResultPresetResponse;
import com.example.ams.api.dto.CreateClinicResultPresetRequest;
import com.example.ams.api.dto.UpdateClinicResultPresetRequest;
import com.example.ams.common.ApiResponse;
import com.example.ams.service.ClinicResultPresetService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/classes/{classId}/clinic/presets")
public class ClassClinicPresetController {

	private final ClinicResultPresetService presetService;

	public ClassClinicPresetController(ClinicResultPresetService presetService) {
		this.presetService = presetService;
	}

	@GetMapping
	public ApiResponse<List<ClinicResultPresetResponse>> listPresets(@PathVariable long classId) {
		List<ClinicResultPresetResponse> presets = presetService.listPresets(classId).stream()
				.map(ClinicResultPresetResponse::from)
				.toList();
		return ApiResponse.ok(presets);
	}

	@GetMapping("/{presetId}")
	public ApiResponse<ClinicResultPresetResponse> getPreset(
			@PathVariable long classId,
			@PathVariable long presetId) {
		return ApiResponse.ok(ClinicResultPresetResponse.from(presetService.getPreset(classId, presetId)));
	}

	@PostMapping
	public ApiResponse<ClinicResultPresetResponse> createPreset(
			@PathVariable long classId,
			@Valid @RequestBody CreateClinicResultPresetRequest request) {
		return ApiResponse.ok(ClinicResultPresetResponse.from(
				presetService.createPreset(classId, request.name(), request.fields())));
	}

	@PatchMapping("/{presetId}")
	public ApiResponse<ClinicResultPresetResponse> updatePreset(
			@PathVariable long classId,
			@PathVariable long presetId,
			@Valid @RequestBody UpdateClinicResultPresetRequest request) {
		return ApiResponse.ok(ClinicResultPresetResponse.from(
				presetService.updatePreset(classId, presetId, request.name(), request.fields())));
	}

	@DeleteMapping("/{presetId}")
	public ApiResponse<Void> deletePreset(@PathVariable long classId, @PathVariable long presetId) {
		presetService.deletePreset(classId, presetId);
		return ApiResponse.ok(null);
	}
}
