package com.example.ams.api.clazz;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ams.api.dto.CreateHomeworkRequest;
import com.example.ams.api.dto.GradeHomeworkSubmissionRequest;
import com.example.ams.api.dto.HomeworkAnswerKeyResponse;
import com.example.ams.api.dto.HomeworkResponse;
import com.example.ams.api.dto.HomeworkSubmissionResponse;
import com.example.ams.api.dto.SaveAssignmentTargetRequest;
import com.example.ams.api.dto.SaveHomeworkAnswerKeyRequest;
import com.example.ams.api.dto.UpdateHomeworkSubmissionRequest;
import com.example.ams.common.ApiResponse;
import com.example.ams.domain.clazz.Homework;
import com.example.ams.service.HomeworkService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/classes/{classId}/homeworks")
public class ClassHomeworkController {

	private final HomeworkService homeworkService;

	public ClassHomeworkController(HomeworkService homeworkService) {
		this.homeworkService = homeworkService;
	}

	@GetMapping
	public ApiResponse<List<HomeworkResponse>> list(@PathVariable long classId) {
		List<HomeworkResponse> list = homeworkService.listHomeworks(classId).stream()
				.map(this::toResponse)
				.toList();
		return ApiResponse.ok(list);
	}

	@PostMapping
	public ApiResponse<HomeworkResponse> create(
			@PathVariable long classId,
			@Valid @RequestBody CreateHomeworkRequest request) {
		Homework created = homeworkService.createHomework(
				classId,
				request.title(),
				request.questionCount(),
				request.targetStudentIds());
		return ApiResponse.ok(toResponse(created));
	}

	@PutMapping("/{homeworkId}/targets")
	public ApiResponse<HomeworkResponse> saveTargets(
			@PathVariable long homeworkId,
			@Valid @RequestBody SaveAssignmentTargetRequest request) {
		Homework homework = homeworkService.saveTargets(homeworkId, request.studentIds());
		return ApiResponse.ok(toResponse(homework));
	}

	@GetMapping("/{homeworkId}/answer-keys")
	public ApiResponse<HomeworkAnswerKeyResponse> getAnswerKeys(@PathVariable long homeworkId) {
		var homework = homeworkService.getHomework(homeworkId);
		return ApiResponse.ok(HomeworkAnswerKeyResponse.from(
				homework, homeworkService.getAnswerKeys(homeworkId)));
	}

	@PutMapping("/{homeworkId}/answer-keys")
	public ApiResponse<HomeworkAnswerKeyResponse> saveAnswerKeys(
			@PathVariable long homeworkId,
			@Valid @RequestBody SaveHomeworkAnswerKeyRequest request) {
		var homework = homeworkService.saveAnswerKeys(
				homeworkId, request.questionCount(), request.answers());
		return ApiResponse.ok(HomeworkAnswerKeyResponse.from(
				homework, homeworkService.getAnswerKeys(homeworkId)));
	}

	@GetMapping("/{homeworkId}/submissions")
	public ApiResponse<List<HomeworkSubmissionResponse>> listSubmissions(@PathVariable long homeworkId) {
		List<HomeworkSubmissionResponse> rows = homeworkService.listSubmissionRows(homeworkId).stream()
				.map(HomeworkSubmissionResponse::from)
				.toList();
		return ApiResponse.ok(rows);
	}

	@PatchMapping("/{homeworkId}/submissions/{studentId}")
	public ApiResponse<HomeworkSubmissionResponse> updateSubmission(
			@PathVariable long homeworkId,
			@PathVariable long studentId,
			@Valid @RequestBody UpdateHomeworkSubmissionRequest request) {
		homeworkService.updateSubmission(
				homeworkId,
				studentId,
				request.submitted(),
				request.submittedAt(),
				request.score(),
				request.grade(),
				request.memo());
		var row = homeworkService.listSubmissionRows(homeworkId).stream()
				.filter(r -> r.studentId() == studentId)
				.findFirst()
				.orElseThrow();
		return ApiResponse.ok(HomeworkSubmissionResponse.from(row));
	}

	@PatchMapping("/{homeworkId}/submissions/{studentId}/grade")
	public ApiResponse<HomeworkSubmissionResponse> gradeSubmission(
			@PathVariable long homeworkId,
			@PathVariable long studentId,
			@Valid @RequestBody GradeHomeworkSubmissionRequest request) {
		homeworkService.gradeSubmission(homeworkId, studentId, request.answers());
		var row = homeworkService.listSubmissionRows(homeworkId).stream()
				.filter(r -> r.studentId() == studentId)
				.findFirst()
				.orElseThrow();
		return ApiResponse.ok(HomeworkSubmissionResponse.from(row));
	}

	@PatchMapping("/{homeworkId}/complete")
	public ApiResponse<HomeworkResponse> complete(@PathVariable long homeworkId) {
		return ApiResponse.ok(toResponse(homeworkService.markCompleted(homeworkId)));
	}

	private HomeworkResponse toResponse(Homework homework) {
		return HomeworkResponse.from(homework, homeworkService.getTargets(homework.homeworkId()));
	}
}
