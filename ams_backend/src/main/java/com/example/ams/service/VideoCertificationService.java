package com.example.ams.service;

import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.ams.common.BusinessException;
import com.example.ams.common.ErrorCode;
import com.example.ams.domain.clazz.VideoCertification;
import com.example.ams.domain.clazz.VideoLesson;
import com.example.ams.domain.user.UserRole;
import com.example.ams.event.VideoCertificationSubmittedEvent;
import com.example.ams.repository.VideoCertificationRepository;
import com.example.ams.repository.VideoLessonRepository;
import com.example.ams.security.CurrentUserService;

@Service
public class VideoCertificationService {

	private final VideoCertificationRepository certificationRepository;
	private final VideoLessonRepository videoRepository;
	private final ClassAccessService classAccessService;
	private final AssignmentTargetService assignmentTargetService;
	private final CurrentUserService currentUserService;
	private final LocalFileStorageService fileStorageService;
	private final ApplicationEventPublisher eventPublisher;

	public VideoCertificationService(
			VideoCertificationRepository certificationRepository,
			VideoLessonRepository videoRepository,
			ClassAccessService classAccessService,
			AssignmentTargetService assignmentTargetService,
			CurrentUserService currentUserService,
			LocalFileStorageService fileStorageService,
			ApplicationEventPublisher eventPublisher) {
		this.certificationRepository = certificationRepository;
		this.videoRepository = videoRepository;
		this.classAccessService = classAccessService;
		this.assignmentTargetService = assignmentTargetService;
		this.currentUserService = currentUserService;
		this.fileStorageService = fileStorageService;
		this.eventPublisher = eventPublisher;
	}

	public VideoCertification getMyCertification(long videoId) {
		VideoLesson video = requireReadableVideo(videoId);
		if (currentUserService.requireRole() != UserRole.STUDENT) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
		classAccessService.requireReadableClass(video.classId());
		long studentId = currentUserService.requireUserId();
		if (!assignmentTargetService.requiresVideoCertification(
				video.videoId(), video.classId(), studentId)) {
			return null;
		}
		return certificationRepository
				.findByVideoIdAndStudentId(videoId, studentId)
				.orElse(null);
	}

	public List<VideoCertification> listCertifications(long videoId) {
		VideoLesson video = requireReadableVideo(videoId);
		classAccessService.requireReadableClass(video.classId());
		UserRole role = currentUserService.requireRole();
		if (role == UserRole.STUDENT) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
		return certificationRepository.findByVideoId(videoId);
	}

	@Transactional
	public VideoCertification submitCertification(long videoId, MultipartFile file) {
		if (currentUserService.requireRole() != UserRole.STUDENT) {
			throw new BusinessException(ErrorCode.FORBIDDEN, "학생만 인증사진을 제출할 수 있습니다.");
		}
		VideoLesson video = requireReadableVideo(videoId);
		classAccessService.requireReadableClass(video.classId());

		long studentId = currentUserService.requireUserId();
		requireTargetStudent(video, studentId);
		long academyId = currentUserService.requireAcademyId();
		String imageUrl = fileStorageService.storeCertificationImage(academyId, studentId, file);

		VideoCertification saved;
		var existing = certificationRepository.findByVideoIdAndStudentId(videoId, studentId);
		if (existing.isPresent()) {
			certificationRepository.updateImage(existing.get().certificationId(), imageUrl);
			saved = certificationRepository.findByVideoIdAndStudentId(videoId, studentId).orElseThrow();
		} else {
			try {
				saved = certificationRepository.insert(videoId, studentId, imageUrl);
			} catch (DuplicateKeyException ex) {
				certificationRepository.updateImage(
						certificationRepository.findByVideoIdAndStudentId(videoId, studentId)
								.orElseThrow()
								.certificationId(),
						imageUrl);
				saved = certificationRepository.findByVideoIdAndStudentId(videoId, studentId).orElseThrow();
			}
		}
		eventPublisher.publishEvent(new VideoCertificationSubmittedEvent(
				video.classId(), videoId, studentId, video.title()));
		return saved;
	}

	private VideoLesson requireReadableVideo(long videoId) {
		return videoRepository.findById(videoId)
				.orElseThrow(() -> new BusinessException(ErrorCode.VIDEO_NOT_FOUND));
	}

	private void requireTargetStudent(VideoLesson video, long studentId) {
		if (!assignmentTargetService.requiresVideoCertification(
				video.videoId(), video.classId(), studentId)) {
			throw new BusinessException(ErrorCode.FORBIDDEN, "영상 인증 대상 학생만 제출할 수 있습니다.");
		}
	}
}
