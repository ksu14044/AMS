package com.example.ams.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ams.common.BusinessException;
import com.example.ams.common.ErrorCode;
import com.example.ams.domain.link.ParentStudentLink;
import com.example.ams.domain.user.User;
import com.example.ams.domain.user.UserRole;
import com.example.ams.repository.ClassEnrollmentRepository;
import com.example.ams.repository.ParentStudentLinkRepository;
import com.example.ams.repository.UserRepository;
import com.example.ams.security.CurrentUserService;

@Service
public class ParentStudentLinkService {

	private final ParentStudentLinkRepository linkRepository;
	private final UserRepository userRepository;
	private final ClassEnrollmentRepository enrollmentRepository;
	private final CurrentUserService currentUserService;

	public ParentStudentLinkService(
			ParentStudentLinkRepository linkRepository,
			UserRepository userRepository,
			ClassEnrollmentRepository enrollmentRepository,
			CurrentUserService currentUserService) {
		this.linkRepository = linkRepository;
		this.userRepository = userRepository;
		this.enrollmentRepository = enrollmentRepository;
		this.currentUserService = currentUserService;
	}

	@Transactional
	public ParentStudentLink createLink(String parentEmail, long studentId) {
		requireCanManageLinks();
		long academyId = currentUserService.requireAcademyId();
		User parent = userRepository.findByAcademyIdAndEmail(academyId, parentEmail.trim().toLowerCase())
				.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "해당 이메일의 학부모 계정이 없습니다."));
		if (parent.role() != UserRole.PARENT) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "학부모(PARENT) 계정만 연결할 수 있습니다.");
		}
		User student = userRepository.findById(studentId)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
		if (student.academyId() != academyId || student.role() != UserRole.STUDENT) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "해당 학원의 학생만 연결할 수 있습니다.");
		}
		if (linkRepository.existsByParentIdAndStudentId(parent.userId(), studentId)) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "이미 연결된 학부모입니다.");
		}
		return linkRepository.insert(parent.userId(), studentId, currentUserService.requireUserId());
	}

	@Transactional
	public void deleteLink(long linkId) {
		requireCanManageLinks();
		ParentStudentLink link = linkRepository.findById(linkId)
				.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "연결 정보를 찾을 수 없습니다."));
		assertLinkInAcademy(link);
		linkRepository.deleteById(linkId);
	}

	public List<ParentStudentLink> listLinksForStudent(long studentId) {
		requireCanManageLinks();
		assertStudentInAcademy(studentId);
		return linkRepository.findByStudentId(studentId);
	}

	public List<ParentStudentLink> listLinksForCurrentParent() {
		long parentId = currentUserService.requireUserId();
		if (currentUserService.requireRole() != UserRole.PARENT) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
		return linkRepository.findByParentId(parentId);
	}

	public void requireParentLinkedToStudent(long parentId, long studentId) {
		if (!linkRepository.existsByParentIdAndStudentId(parentId, studentId)) {
			throw new BusinessException(ErrorCode.FORBIDDEN, "연결된 자녀만 조회할 수 있습니다.");
		}
	}

	public void requireParentCanAccessClass(long parentId, long studentId, long classId) {
		requireParentLinkedToStudent(parentId, studentId);
		if (!enrollmentRepository.existsByClassIdAndStudentId(classId, studentId)) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
	}

	private void requireCanManageLinks() {
		UserRole role = currentUserService.requireRole();
		if (role == UserRole.ACADEMY_ADMIN || role.isHomeroomTeacher() || role.isAssistant() || role.isOfficeStaff()) {
			return;
		}
		throw new BusinessException(ErrorCode.FORBIDDEN);
	}

	private void assertStudentInAcademy(long studentId) {
		User student = userRepository.findById(studentId).orElseThrow();
		if (student.academyId() != currentUserService.requireAcademyId() || student.role() != UserRole.STUDENT) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST);
		}
	}

	private void assertLinkInAcademy(ParentStudentLink link) {
		User parent = userRepository.findById(link.parentId()).orElseThrow();
		User student = userRepository.findById(link.studentId()).orElseThrow();
		long academyId = currentUserService.requireAcademyId();
		if (parent.academyId() != academyId || student.academyId() != academyId) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
	}
}
