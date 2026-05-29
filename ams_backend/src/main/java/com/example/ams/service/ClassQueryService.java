package com.example.ams.service;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.ams.common.BusinessException;
import com.example.ams.common.ErrorCode;
import com.example.ams.domain.clazz.Clazz;
import com.example.ams.domain.user.UserRole;
import com.example.ams.repository.ClazzRepository;
import com.example.ams.security.CurrentUserService;

@Service
public class ClassQueryService {

	private final ClazzRepository clazzRepository;
	private final CurrentUserService currentUserService;

	public ClassQueryService(ClazzRepository clazzRepository, CurrentUserService currentUserService) {
		this.clazzRepository = clazzRepository;
		this.currentUserService = currentUserService;
	}

	public List<Clazz> listForCurrentUser() {
		UserRole role = currentUserService.requireRole();
		long userId = currentUserService.requireUserId();
		long academyId = currentUserService.requireAcademyId();

		if (role == UserRole.ACADEMY_ADMIN) {
			return clazzRepository.findByAcademyId(academyId);
		}
		if (role == UserRole.STUDENT) {
			return clazzRepository.findByStudentId(userId);
		}
		if (role.isHomeroomTeacher()) {
			return clazzRepository.findByHomeroomTeacherId(userId);
		}
		if (role.isAssistant()) {
			return clazzRepository.findByAssistantId(userId);
		}
		if (role.isOfficeStaff()) {
			return Collections.emptyList();
		}
		throw new BusinessException(ErrorCode.FORBIDDEN);
	}
}
