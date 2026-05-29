package com.example.ams.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.ams.common.BusinessException;
import com.example.ams.common.ErrorCode;
import com.example.ams.domain.clazz.Clazz;
import com.example.ams.domain.user.UserRole;
import com.example.ams.security.CurrentUserService;

@Service
public class StudentClassService {

	private final ClassQueryService classQueryService;
	private final CurrentUserService currentUserService;

	public StudentClassService(ClassQueryService classQueryService, CurrentUserService currentUserService) {
		this.classQueryService = classQueryService;
		this.currentUserService = currentUserService;
	}

	public List<Clazz> listMyClasses() {
		if (currentUserService.requireRole() != UserRole.STUDENT) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
		return classQueryService.listForCurrentUser();
	}
}
