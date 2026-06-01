package com.example.ams.domain.clazz;

import java.util.List;

public record ClinicResultFieldDef(
		String key,
		String label,
		String type,
		Boolean required,
		Integer maxLength,
		List<String> options) {
}
