package com.example.ams.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ams.common.BusinessException;
import com.example.ams.common.ErrorCode;
import com.example.ams.domain.clazz.Clazz;
import com.example.ams.domain.report.ReportPeriodPreset;
import com.example.ams.repository.ReportPeriodPresetRepository;

@Service
public class ReportPeriodPresetService {

	private final ReportPeriodPresetRepository presetRepository;
	private final ClassAccessService classAccessService;

	public ReportPeriodPresetService(
			ReportPeriodPresetRepository presetRepository,
			ClassAccessService classAccessService) {
		this.presetRepository = presetRepository;
		this.classAccessService = classAccessService;
	}

	public List<ReportPeriodPreset> listPresets(long classId) {
		Clazz clazz = classAccessService.requireReadableClass(classId);
		requireCanManageReports(clazz);
		return presetRepository.findByClassId(classId);
	}

	@Transactional
	public ReportPeriodPreset createPreset(long classId, String name, LocalDate periodStart, LocalDate periodEnd) {
		Clazz clazz = classAccessService.requireReadableClass(classId);
		requireCanManageReports(clazz);
		validatePeriod(periodStart, periodEnd);
		return presetRepository.insert(classId, requireName(name), periodStart, periodEnd);
	}

	@Transactional
	public ReportPeriodPreset updatePreset(
			long classId,
			long presetId,
			String name,
			LocalDate periodStart,
			LocalDate periodEnd) {
		Clazz clazz = classAccessService.requireReadableClass(classId);
		requireCanManageReports(clazz);
		requirePreset(classId, presetId);
		validatePeriod(periodStart, periodEnd);
		return presetRepository.update(presetId, classId, requireName(name), periodStart, periodEnd);
	}

	@Transactional
	public void deletePreset(long classId, long presetId) {
		Clazz clazz = classAccessService.requireReadableClass(classId);
		requireCanManageReports(clazz);
		requirePreset(classId, presetId);
		presetRepository.delete(presetId, classId);
	}

	public ReportPeriodPreset requirePreset(long classId, long presetId) {
		return presetRepository.findByIdAndClassId(presetId, classId)
				.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "보고서 기간 프리셋을 찾을 수 없습니다."));
	}

	private void requireCanManageReports(Clazz clazz) {
		if (!classAccessService.canManageClassContent(clazz)) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
	}

	private static String requireName(String name) {
		if (name == null || name.isBlank()) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "프리셋 이름을 입력하세요.");
		}
		return name.trim();
	}

	private static void validatePeriod(LocalDate periodStart, LocalDate periodEnd) {
		if (periodEnd.isBefore(periodStart)) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "기간 종료일은 시작일 이후여야 합니다.");
		}
	}
}
