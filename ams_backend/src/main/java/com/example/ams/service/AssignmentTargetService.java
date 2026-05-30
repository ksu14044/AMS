package com.example.ams.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.ams.common.BusinessException;
import com.example.ams.common.ErrorCode;
import com.example.ams.domain.clazz.AssignmentEntityType;
import com.example.ams.repository.AssignmentTargetRepository;
import com.example.ams.repository.ClassEnrollmentRepository;

@Service
public class AssignmentTargetService {

	private final AssignmentTargetRepository targetRepository;
	private final ClassEnrollmentRepository enrollmentRepository;

	public AssignmentTargetService(
			AssignmentTargetRepository targetRepository,
			ClassEnrollmentRepository enrollmentRepository) {
		this.targetRepository = targetRepository;
		this.enrollmentRepository = enrollmentRepository;
	}

	public void saveExplicitTargets(
			AssignmentEntityType entityType,
			long entityId,
			long classId,
			List<Long> studentIds) {
		validateEnrolled(classId, studentIds);
		targetRepository.replaceAll(entityType, entityId, studentIds);
	}

	public void applyOnCreate(
			AssignmentEntityType entityType,
			long entityId,
			long classId,
			List<Long> targetStudentIds) {
		if (targetStudentIds == null || targetStudentIds.isEmpty()) {
			return;
		}
		saveExplicitTargets(entityType, entityId, classId, targetStudentIds);
	}

	public List<Long> resolveTargetStudentIds(AssignmentEntityType entityType, long entityId, long classId) {
		List<Long> explicit = targetRepository.findStudentIdsByEntity(entityType, entityId);
		if (!explicit.isEmpty()) {
			return explicit;
		}
		if (entityType == AssignmentEntityType.VIDEO) {
			return List.of();
		}
		return enrollmentRepository.findByClassId(classId).stream()
				.map(e -> e.studentId())
				.toList();
	}

	/** 영상: assignment_target = 인증 제출 대상 (시청 권한과 무관) */
	public boolean requiresVideoCertification(long videoId, long classId, long studentId) {
		return targetRepository.findStudentIdsByEntity(AssignmentEntityType.VIDEO, videoId).contains(studentId);
	}

	public boolean canStudentAccess(AssignmentEntityType entityType, long entityId, long classId, long studentId) {
		return resolveTargetStudentIds(entityType, entityId, classId).contains(studentId);
	}

	public TargetView getTargetView(AssignmentEntityType entityType, long entityId, long classId) {
		boolean explicit = targetRepository.hasExplicitTargets(entityType, entityId);
		List<Long> effective = resolveTargetStudentIds(entityType, entityId, classId);
		boolean allClassTargeted = !explicit && entityType != AssignmentEntityType.VIDEO;
		return new TargetView(effective, allClassTargeted);
	}

	private void validateEnrolled(long classId, List<Long> studentIds) {
		for (long studentId : studentIds) {
			if (!enrollmentRepository.existsByClassIdAndStudentId(classId, studentId)) {
				throw new BusinessException(ErrorCode.INVALID_REQUEST, "수강생만 대상으로 지정할 수 있습니다.");
			}
		}
	}

	public record TargetView(List<Long> targetStudentIds, boolean allClassTargeted) {
	}
}
