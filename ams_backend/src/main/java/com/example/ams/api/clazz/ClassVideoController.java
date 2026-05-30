package com.example.ams.api.clazz;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ams.api.dto.CreateVideoLessonRequest;
import com.example.ams.api.dto.SaveAssignmentTargetRequest;
import com.example.ams.api.dto.UpdateVideoLessonRequest;
import com.example.ams.api.dto.VideoLessonResponse;
import com.example.ams.common.ApiResponse;
import com.example.ams.domain.clazz.VideoLesson;
import com.example.ams.service.VideoLessonService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/classes/{classId}/videos")
public class ClassVideoController {

	private final VideoLessonService videoLessonService;

	public ClassVideoController(VideoLessonService videoLessonService) {
		this.videoLessonService = videoLessonService;
	}

	@GetMapping
	public ApiResponse<List<VideoLessonResponse>> list(@PathVariable long classId) {
		List<VideoLessonResponse> list = videoLessonService.listVideos(classId).stream()
				.map(this::toResponse)
				.toList();
		return ApiResponse.ok(list);
	}

	@PostMapping
	public ApiResponse<VideoLessonResponse> create(
			@PathVariable long classId,
			@Valid @RequestBody CreateVideoLessonRequest request) {
		VideoLesson created = videoLessonService.createVideo(
				classId,
				request.youtubeUrl(),
				request.title(),
				request.description(),
				request.publishedAt(),
				request.targetStudentIds());
		return ApiResponse.ok(toResponse(created));
	}

	@PutMapping("/{videoId}/targets")
	public ApiResponse<VideoLessonResponse> saveTargets(
			@PathVariable long videoId,
			@Valid @RequestBody SaveAssignmentTargetRequest request) {
		VideoLesson video = videoLessonService.saveTargets(videoId, request.studentIds());
		return ApiResponse.ok(toResponse(video));
	}

	@PatchMapping("/{videoId}")
	public ApiResponse<VideoLessonResponse> update(
			@PathVariable long classId,
			@PathVariable long videoId,
			@Valid @RequestBody UpdateVideoLessonRequest request) {
		VideoLesson updated = videoLessonService.updateVideo(
				classId,
				videoId,
				request.youtubeUrl(),
				request.title(),
				request.description());
		return ApiResponse.ok(toResponse(updated));
	}

	@DeleteMapping("/{videoId}")
	public ApiResponse<Void> delete(@PathVariable long classId, @PathVariable long videoId) {
		videoLessonService.deleteVideo(classId, videoId);
		return ApiResponse.ok(null);
	}

	private VideoLessonResponse toResponse(VideoLesson video) {
		return VideoLessonResponse.from(video, videoLessonService.getTargets(video.videoId()));
	}
}
