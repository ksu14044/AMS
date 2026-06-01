package com.example.ams.api.dto;

import java.util.List;

import com.example.ams.common.ClinicResultSchemaJson;
import com.example.ams.domain.clazz.ClinicResultPreset;

public record ClinicResultPresetResponse(
		long presetId,
		long classId,
		String name,
		List<ClinicResultFieldResponse> fields) {

	public static ClinicResultPresetResponse from(ClinicResultPreset preset) {
		return new ClinicResultPresetResponse(
				preset.presetId(),
				preset.classId(),
				preset.name(),
				ClinicResultSchemaJson.parseFields(preset.fieldSchemaJson()).stream()
						.map(ClinicResultFieldResponse::from)
						.toList());
	}
}
