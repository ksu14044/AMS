package com.example.ams.api.dto;

import java.time.Instant;

import com.example.ams.domain.clazz.VideoLesson;
import com.example.ams.service.AssignmentTargetService.TargetView;

public record VideoLessonResponse(
		long videoId,
		long classId,
		String youtubeUrl,
		String title,
		String description,
		String thumbnailUrl,
		Instant publishedAt,
		long authorId,
		boolean thumbnailAvailable,
		AssignmentTargetResponse targets) {

	public static VideoLessonResponse from(VideoLesson video, TargetView targets) {
		return new VideoLessonResponse(
				video.videoId(),
				video.classId(),
				video.youtubeUrl(),
				video.title(),
				video.description(),
				video.thumbnailUrl(),
				video.publishedAt(),
				video.authorId(),
				video.thumbnailUrl() != null && !video.thumbnailUrl().isBlank(),
				AssignmentTargetResponse.from(targets));
	}
}
