package com.example.ams.api.dto;

import java.util.List;

import com.example.ams.domain.clazz.ClinicResultFieldDef;
import com.example.ams.domain.clazz.ClinicResultPreset;

public record ClinicResultFieldResponse(
		String key,
		String label,
		String type,
		boolean required,
		Integer maxLength,
		List<String> options) {

	public static ClinicResultFieldResponse from(ClinicResultFieldDef field) {
		return new ClinicResultFieldResponse(
				field.key(),
				field.label(),
				field.type(),
				Boolean.TRUE.equals(field.required()),
				field.maxLength(),
				field.options());
	}
}
