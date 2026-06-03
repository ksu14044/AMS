package com.example.ams.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ams.common.BusinessException;
import com.example.ams.common.ErrorCode;
import com.example.ams.domain.user.Subject;
import com.example.ams.domain.user.User;
import com.example.ams.domain.user.UserRole;
import com.example.ams.domain.user.UserStatus;
import com.example.ams.repository.UserRepository;
import com.example.ams.security.CurrentUserService;

@Service
public class AdminUserService {

	private final UserRepository userRepository;
	private final CurrentUserService currentUserService;

	public AdminUserService(UserRepository userRepository, CurrentUserService currentUserService) {
		this.userRepository = userRepository;
		this.currentUserService = currentUserService;
	}

	public List<User> listPendingStaff() {
		long academyId = currentUserService.requireAcademyId();
		return userRepository.findPendingStaffByAcademyId(academyId);
	}

	@Transactional
	public User approve(long userId, UserRole role, Subject subject) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
		currentUserService.assertSameAcademy(user.academyId());
		if (user.status() != UserStatus.PENDING) {
			throw new BusinessException(ErrorCode.USER_NOT_PENDING);
		}
		if (role == UserRole.STUDENT || role == UserRole.PARENT || role == UserRole.ACADEMY_ADMIN) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "승인 시 교직원 역할만 지정할 수 있습니다.");
		}
		if (role.requiresSubject() && subject == null) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "선생님·조교는 과목을 지정해야 합니다.");
		}
		return userRepository.updateRoleAndStatus(userId, role, subject, UserStatus.ACTIVE);
	}
}
