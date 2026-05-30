package com.example.ams.service;

import org.springframework.stereotype.Service;

import com.example.ams.common.BusinessException;
import com.example.ams.common.ErrorCode;
import com.example.ams.domain.clazz.Clazz;
import com.example.ams.domain.user.UserRole;
import com.example.ams.repository.AssistantClassAssignmentRepository;
import com.example.ams.repository.ClassEnrollmentRepository;
import com.example.ams.repository.ClinicSlotRepository;
import com.example.ams.repository.ClazzRepository;
import com.example.ams.security.CurrentUserService;

@Service
public class ClassAccessService {

	private final ClazzRepository clazzRepository;
	private final ClassEnrollmentRepository enrollmentRepository;
	private final AssistantClassAssignmentRepository assistantAssignmentRepository;
	private final ClinicSlotRepository clinicSlotRepository;
	private final CurrentUserService currentUserService;

	public ClassAccessService(
			ClazzRepository clazzRepository,
			ClassEnrollmentRepository enrollmentRepository,
			AssistantClassAssignmentRepository assistantAssignmentRepository,
			ClinicSlotRepository clinicSlotRepository,
			CurrentUserService currentUserService) {
		this.clazzRepository = clazzRepository;
		this.enrollmentRepository = enrollmentRepository;
		this.assistantAssignmentRepository = assistantAssignmentRepository;
		this.clinicSlotRepository = clinicSlotRepository;
		this.currentUserService = currentUserService;
	}

	public Clazz requireReadableClass(long classId) {
		Clazz clazz = clazzRepository.findById(classId)
				.orElseThrow(() -> new BusinessException(ErrorCode.CLASS_NOT_FOUND));
		currentUserService.assertSameAcademy(clazz.academyId());

		UserRole role = currentUserService.requireRole();
		long userId = currentUserService.requireUserId();

		if (role == UserRole.ACADEMY_ADMIN) {
			return clazz;
		}
		if (role == UserRole.STUDENT) {
			if (!enrollmentRepository.existsByClassIdAndStudentId(classId, userId)) {
				throw new BusinessException(ErrorCode.FORBIDDEN);
			}
			return clazz;
		}
		if (role.isHomeroomTeacher()) {
			if (clazz.homeroomTeacherId() != userId) {
				throw new BusinessException(ErrorCode.FORBIDDEN);
			}
			return clazz;
		}
		if (role.isAssistant()) {
			if (!assistantAssignmentRepository.existsByClassIdAndAssistantId(classId, userId)) {
				throw new BusinessException(ErrorCode.FORBIDDEN);
			}
			return clazz;
		}
		throw new BusinessException(ErrorCode.FORBIDDEN);
	}

	/** 담당 반 배정 또는 해당 반 클리닉 슬롯 조교 — 클리닉 조회·출결용 */
	public Clazz requireClinicReadableClass(long classId) {
		Clazz clazz = clazzRepository.findById(classId)
				.orElseThrow(() -> new BusinessException(ErrorCode.CLASS_NOT_FOUND));
		currentUserService.assertSameAcademy(clazz.academyId());

		UserRole role = currentUserService.requireRole();
		long userId = currentUserService.requireUserId();

		if (role == UserRole.ACADEMY_ADMIN) {
			return clazz;
		}
		if (role.isHomeroomTeacher()) {
			if (clazz.homeroomTeacherId() != userId) {
				throw new BusinessException(ErrorCode.FORBIDDEN);
			}
			return clazz;
		}
		if (role.isAssistant()) {
			boolean assigned = assistantAssignmentRepository.existsByClassIdAndAssistantId(classId, userId);
			boolean clinicAssistant = clinicSlotRepository.existsByClassIdAndAssistantId(classId, userId);
			if (!assigned && !clinicAssistant) {
				throw new BusinessException(ErrorCode.FORBIDDEN);
			}
			return clazz;
		}
		if (role == UserRole.STUDENT) {
			if (!enrollmentRepository.existsByClassIdAndStudentId(classId, userId)) {
				throw new BusinessException(ErrorCode.FORBIDDEN);
			}
			return clazz;
		}
		throw new BusinessException(ErrorCode.FORBIDDEN);
	}

	public boolean canManageClassContent(Clazz clazz) {
		UserRole role = currentUserService.requireRole();
		if (role == UserRole.ACADEMY_ADMIN) {
			return true;
		}
		return role.isHomeroomTeacher() && clazz.homeroomTeacherId() == currentUserService.requireUserId();
	}

	public void requireManageClassContent(Clazz clazz) {
		if (!canManageClassContent(clazz)) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
	}

	/** 담임·관리자·담당 조교 — 수업기록·숙제 등 콘텐츠 편집 */
	public boolean canEditClassContent(Clazz clazz) {
		UserRole role = currentUserService.requireRole();
		long userId = currentUserService.requireUserId();
		if (role == UserRole.ACADEMY_ADMIN) {
			return true;
		}
		if (role.isHomeroomTeacher() && clazz.homeroomTeacherId() == userId) {
			return true;
		}
		return role.isAssistant()
				&& assistantAssignmentRepository.existsByClassIdAndAssistantId(clazz.classId(), userId);
	}

	public void requireEditClassContent(Clazz clazz) {
		if (!canEditClassContent(clazz)) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
	}
}
