package com.example.ams.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.ams.common.BusinessException;
import com.example.ams.common.ErrorCode;
import com.example.ams.domain.user.StudentRosterRow;
import com.example.ams.domain.user.UserRole;
import com.example.ams.repository.StudentRosterRepository;
import com.example.ams.security.CurrentUserService;

@Service
public class StudentRosterService {

	private final StudentRosterRepository studentRosterRepository;
	private final CurrentUserService currentUserService;

	public StudentRosterService(
			StudentRosterRepository studentRosterRepository,
			CurrentUserService currentUserService) {
		this.studentRosterRepository = studentRosterRepository;
		this.currentUserService = currentUserService;
	}

	public List<StudentRosterRow> listRoster(String search) {
		UserRole role = currentUserService.requireRole();
		long academyId = currentUserService.requireAcademyId();
		long userId = currentUserService.requireUserId();

		if (role == UserRole.ACADEMY_ADMIN || role == UserRole.STAFF_OFFICE) {
			return studentRosterRepository.findForAcademy(academyId, search);
		}
		if (role.isHomeroomTeacher()) {
			return studentRosterRepository.findForHomeroomTeacher(academyId, userId, search);
		}
		throw new BusinessException(ErrorCode.FORBIDDEN);
	}
}
