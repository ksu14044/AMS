package com.example.ams.domain.clazz;

public record ClinicResultPreset(
		long presetId,
		long classId,
		String name,
		String fieldSchemaJson) {
}
