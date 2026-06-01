package com.example.ams.api.dto;

import java.util.List;

import com.example.ams.domain.clazz.ClinicResultFieldDef;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record UpdateClinicResultPresetRequest(
		@NotBlank @Size(max = 100) String name,
		@NotEmpty @Valid List<ClinicResultFieldDef> fields) {
}
