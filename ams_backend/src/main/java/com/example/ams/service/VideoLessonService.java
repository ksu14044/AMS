package com.example.ams.service;

import java.time.Instant;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ams.common.BusinessException;
import com.example.ams.common.ErrorCode;
import com.example.ams.common.YoutubeUrlValidator;
import com.example.ams.domain.clazz.AssignmentEntityType;
import com.example.ams.domain.clazz.Clazz;
import com.example.ams.domain.clazz.VideoLesson;
import com.example.ams.event.VideoLessonCreatedEvent;
import com.example.ams.integration.YoutubeOEmbedClient;
import com.example.ams.integration.YoutubeOEmbedMetadata;
import com.example.ams.repository.VideoLessonRepository;
import com.example.ams.security.CurrentUserService;

@Service
public class VideoLessonService {

	private final VideoLessonRepository videoRepository;
	private final ClassAccessService classAccessService;
	private final CurrentUserService currentUserService;
	private final AssignmentTargetService assignmentTargetService;
	private final YoutubeOEmbedClient youtubeOEmbedClient;
	private final ApplicationEventPublisher eventPublisher;

	public VideoLessonService(
			VideoLessonRepository videoRepository,
			ClassAccessService classAccessService,
			CurrentUserService currentUserService,
			AssignmentTargetService assignmentTargetService,
			YoutubeOEmbedClient youtubeOEmbedClient,
			ApplicationEventPublisher eventPublisher) {
		this.videoRepository = videoRepository;
		this.classAccessService = classAccessService;
		this.currentUserService = currentUserService;
		this.assignmentTargetService = assignmentTargetService;
		this.youtubeOEmbedClient = youtubeOEmbedClient;
		this.eventPublisher = eventPublisher;
	}

	public List<VideoLesson> listVideos(long classId) {
		classAccessService.requireReadableClass(classId);
		return videoRepository.findByClassId(classId);
	}

	public AssignmentTargetService.TargetView getTargets(long videoId) {
		VideoLesson video = requireVideoInClass(videoId);
		return assignmentTargetService.getTargetView(AssignmentEntityType.VIDEO, videoId, video.classId());
	}

	private VideoLesson requireVideoInClass(long videoId) {
		VideoLesson video = videoRepository.findById(videoId)
				.orElseThrow(() -> new BusinessException(ErrorCode.VIDEO_NOT_FOUND));
		classAccessService.requireReadableClass(video.classId());
		return video;
	}

	@Transactional
	public VideoLesson createVideo(
			long classId,
			String youtubeUrl,
			String title,
			String description,
			Instant publishedAt,
			List<Long> targetStudentIds) {
		Clazz clazz = classAccessService.requireReadableClass(classId);
		classAccessService.requireManageClassContent(clazz);
		return insertVideo(clazz.classId(), null, youtubeUrl, title, description, publishedAt, targetStudentIds);
	}

	@Transactional
	public VideoLesson createVideoForLessonRecord(
			long classId,
			long lessonRecordId,
			String youtubeUrl,
			String title,
			Instant publishedAt,
			List<Long> targetStudentIds) {
		Clazz clazz = classAccessService.requireReadableClass(classId);
		classAccessService.requireEditClassContent(clazz);
		return insertVideo(clazz.classId(), lessonRecordId, youtubeUrl, title, null, publishedAt, targetStudentIds);
	}

	private VideoLesson insertVideo(
			long classId,
			Long lessonRecordId,
			String youtubeUrl,
			String title,
			String description,
			Instant publishedAt,
			List<Long> targetStudentIds) {
		String normalizedUrl = youtubeUrl.trim();
		YoutubeUrlValidator.requireValid(normalizedUrl);
		String thumbnailUrl = resolveThumbnail(normalizedUrl);
		Instant when = publishedAt != null ? publishedAt : Instant.now();
		VideoLesson created = videoRepository.insert(
				classId,
				lessonRecordId,
				normalizedUrl,
				title,
				description,
				thumbnailUrl,
				when,
				currentUserService.requireUserId());
		assignmentTargetService.applyOnCreate(
				AssignmentEntityType.VIDEO, created.videoId(), classId, targetStudentIds);
		eventPublisher.publishEvent(new VideoLessonCreatedEvent(classId, created.videoId(), title));
		return created;
	}

	@Transactional
	public VideoLesson saveTargets(long videoId, List<Long> studentIds) {
		VideoLesson video = requireVideoInClass(videoId);
		classAccessService.requireEditClassContent(
				classAccessService.requireReadableClass(video.classId()));
		assignmentTargetService.saveExplicitTargets(
				AssignmentEntityType.VIDEO, videoId, video.classId(), studentIds);
		return videoRepository.findById(videoId).orElseThrow();
	}

	@Transactional
	public VideoLesson updateVideoWithTargets(
			long classId,
			long videoId,
			String youtubeUrl,
			String title,
			List<Long> targetStudentIds) {
		VideoLesson video = updateVideo(classId, videoId, youtubeUrl, title, null);
		assignmentTargetService.updateTargets(
				AssignmentEntityType.VIDEO, videoId, classId, targetStudentIds);
		return videoRepository.findById(videoId).orElseThrow();
	}

	@Transactional
	public void deleteVideoIfAllowed(long classId, long videoId) {
		Clazz clazz = classAccessService.requireReadableClass(classId);
		classAccessService.requireManageClassContent(clazz);
		requireVideoInClass(classId, videoId);
		assignmentTargetService.clearTargets(AssignmentEntityType.VIDEO, videoId);
		videoRepository.delete(videoId, classId);
	}

	@Transactional
	public VideoLesson updateVideo(
			long classId,
			long videoId,
			String youtubeUrl,
			String title,
			String description) {
		Clazz clazz = classAccessService.requireReadableClass(classId);
		classAccessService.requireManageClassContent(clazz);
		String normalizedUrl = youtubeUrl.trim();
		YoutubeUrlValidator.requireValid(normalizedUrl);
		requireVideoInClass(classId, videoId);
		String thumbnailUrl = resolveThumbnail(normalizedUrl);
		return videoRepository.update(videoId, classId, normalizedUrl, title, description, thumbnailUrl);
	}

	@Transactional
	public void deleteVideo(long classId, long videoId) {
		Clazz clazz = classAccessService.requireReadableClass(classId);
		classAccessService.requireManageClassContent(clazz);
		requireVideoInClass(classId, videoId);
		videoRepository.delete(videoId, classId);
	}

	private String resolveThumbnail(String youtubeUrl) {
		return youtubeOEmbedClient.fetch(youtubeUrl)
				.map(YoutubeOEmbedMetadata::thumbnailUrl)
				.orElse(null);
	}

	private VideoLesson requireVideoInClass(long classId, long videoId) {
		return videoRepository.findByIdAndClassId(videoId, classId)
				.orElseThrow(() -> new BusinessException(ErrorCode.VIDEO_NOT_FOUND));
	}
}
