package com.example.ams.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ams.common.BusinessException;
import com.example.ams.common.ErrorCode;
import com.example.ams.domain.academy.AcademyNotice;
import com.example.ams.domain.user.UserRole;
import com.example.ams.repository.AcademyNoticeRepository;
import com.example.ams.security.CurrentUserService;

@Service
public class AcademyNoticeService {

	private final AcademyNoticeRepository noticeRepository;
	private final CurrentUserService currentUserService;

	public AcademyNoticeService(AcademyNoticeRepository noticeRepository, CurrentUserService currentUserService) {
		this.noticeRepository = noticeRepository;
		this.currentUserService = currentUserService;
	}

	public List<AcademyNotice> listNotices() {
		long academyId = currentUserService.requireAcademyId();
		return noticeRepository.findByAcademyId(academyId);
	}

	@Transactional
	public AcademyNotice createNotice(String title, String body, String attachmentUrl) {
		requireManageAcademyNotice();
		long academyId = currentUserService.requireAcademyId();
		return noticeRepository.insert(
				academyId,
				title,
				body,
				attachmentUrl,
				currentUserService.requireUserId());
	}

	private void requireManageAcademyNotice() {
		UserRole role = currentUserService.requireRole();
		if (role != UserRole.ACADEMY_ADMIN && role != UserRole.STAFF_OFFICE) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
	}
}
