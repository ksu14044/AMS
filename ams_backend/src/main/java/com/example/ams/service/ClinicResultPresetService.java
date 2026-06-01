package com.example.ams.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ams.common.BusinessException;
import com.example.ams.common.ClinicResultSchemaJson;
import com.example.ams.common.ErrorCode;
import com.example.ams.domain.clazz.ClinicResultFieldDef;
import com.example.ams.domain.clazz.ClinicResultPreset;
import com.example.ams.domain.clazz.Clazz;
import com.example.ams.repository.ClinicResultPresetRepository;

@Service
public class ClinicResultPresetService {

	private final ClinicResultPresetRepository presetRepository;
	private final ClassAccessService classAccessService;

	public ClinicResultPresetService(
			ClinicResultPresetRepository presetRepository,
			ClassAccessService classAccessService) {
		this.presetRepository = presetRepository;
		this.classAccessService = classAccessService;
	}

	public List<ClinicResultPreset> listPresets(long classId) {
		classAccessService.requireClinicReadableClass(classId);
		return presetRepository.findByClassId(classId);
	}

	public ClinicResultPreset getPreset(long classId, long presetId) {
		classAccessService.requireClinicReadableClass(classId);
		return requirePreset(classId, presetId);
	}

	@Transactional
	public ClinicResultPreset createPreset(long classId, String name, List<ClinicResultFieldDef> fields) {
		Clazz clazz = classAccessService.requireReadableClass(classId);
		classAccessService.requireManageClassContent(clazz);
		String trimmedName = requireName(name);
		String schemaJson = ClinicResultSchemaJson.toJson(fields);
		return presetRepository.insert(classId, trimmedName, schemaJson);
	}

	@Transactional
	public ClinicResultPreset updatePreset(
			long classId,
			long presetId,
			String name,
			List<ClinicResultFieldDef> fields) {
		Clazz clazz = classAccessService.requireReadableClass(classId);
		classAccessService.requireManageClassContent(clazz);
		requirePreset(classId, presetId);
		String trimmedName = requireName(name);
		String schemaJson = ClinicResultSchemaJson.toJson(fields);
		return presetRepository.update(presetId, classId, trimmedName, schemaJson);
	}

	@Transactional
	public void deletePreset(long classId, long presetId) {
		Clazz clazz = classAccessService.requireReadableClass(classId);
		classAccessService.requireManageClassContent(clazz);
		requirePreset(classId, presetId);
		if (presetRepository.countSlotsUsingPreset(presetId) > 0) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "사용 중인 프리셋은 삭제할 수 없습니다.");
		}
		if (presetRepository.findByClassId(classId).size() <= 1) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "마지막 프리셋은 삭제할 수 없습니다.");
		}
		presetRepository.delete(presetId, classId);
	}

	@Transactional
	public ClinicResultPreset ensureDefaultPreset(long classId) {
		return presetRepository.ensureDefaultPreset(classId);
	}

	public ClinicResultPreset requirePreset(long classId, long presetId) {
		return presetRepository.findByIdAndClassId(presetId, classId)
				.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "클리닉 프리셋을 찾을 수 없습니다."));
	}

	public List<ClinicResultFieldDef> parseFields(ClinicResultPreset preset) {
		return ClinicResultSchemaJson.parseFields(preset.fieldSchemaJson());
	}

	private static String requireName(String name) {
		if (name == null || name.isBlank()) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "프리셋 이름을 입력해 주세요.");
		}
		String trimmed = name.trim();
		if (trimmed.length() > 100) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "프리셋 이름은 100자 이하여야 합니다.");
		}
		return trimmed;
	}
}
