package com.example.ams.service;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ams.domain.clazz.ClassNotice;
import com.example.ams.domain.clazz.Clazz;
import com.example.ams.event.ClassNoticeCreatedEvent;
import com.example.ams.repository.ClassNoticeRepository;
import com.example.ams.security.CurrentUserService;

@Service
public class ClassNoticeService {

	private final ClassNoticeRepository noticeRepository;
	private final ClassAccessService classAccessService;
	private final CurrentUserService currentUserService;
	private final ApplicationEventPublisher eventPublisher;

	public ClassNoticeService(
			ClassNoticeRepository noticeRepository,
			ClassAccessService classAccessService,
			CurrentUserService currentUserService,
			ApplicationEventPublisher eventPublisher) {
		this.noticeRepository = noticeRepository;
		this.classAccessService = classAccessService;
		this.currentUserService = currentUserService;
		this.eventPublisher = eventPublisher;
	}

	public List<ClassNotice> listNotices(long classId) {
		classAccessService.requireReadableClass(classId);
		return noticeRepository.findByClassId(classId);
	}

	@Transactional
	public ClassNotice createNotice(long classId, String title, String body, String attachmentUrl) {
		Clazz clazz = classAccessService.requireReadableClass(classId);
		classAccessService.requireManageClassContent(clazz);
		ClassNotice created = noticeRepository.insert(
				clazz.classId(),
				title,
				body,
				attachmentUrl,
				currentUserService.requireUserId());
		eventPublisher.publishEvent(new ClassNoticeCreatedEvent(clazz.classId(), created.noticeId(), title));
		return created;
	}
}
