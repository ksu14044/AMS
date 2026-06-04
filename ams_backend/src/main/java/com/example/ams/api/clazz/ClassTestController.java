package com.example.ams.api.clazz;

import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.ams.api.dto.CreateTestRequest;
import com.example.ams.api.dto.CreateTestRetakeRequest;
import com.example.ams.api.dto.RecordSubmissionResultRequest;
import com.example.ams.api.dto.TestAnswerKeyResponse;
import com.example.ams.api.dto.TestExamResponse;
import com.example.ams.api.dto.TestScoreResponse;
import com.example.ams.api.dto.UpdateTestScoresRequest;
import com.example.ams.common.ApiResponse;
import com.example.ams.domain.clazz.AssignmentStatus;
import com.example.ams.domain.clazz.TestExam;
import com.example.ams.domain.user.UserRole;
import com.example.ams.security.CurrentUserService;
import com.example.ams.service.AnswerKeyPdfStorageService;
import com.example.ams.service.TestExamService;
import com.example.ams.service.TestExamService.ScoreUpdate;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/classes/{classId}/tests")
public class ClassTestController {

	private final TestExamService testExamService;
	private final AnswerKeyPdfStorageService answerKeyStorageService;
	private final CurrentUserService currentUserService;

	public ClassTestController(
			TestExamService testExamService,
			AnswerKeyPdfStorageService answerKeyStorageService,
			CurrentUserService currentUserService) {
		this.testExamService = testExamService;
		this.answerKeyStorageService = answerKeyStorageService;
		this.currentUserService = currentUserService;
	}

	@GetMapping
	public ApiResponse<List<TestExamResponse>> list(@PathVariable long classId) {
		List<TestExamResponse> list = testExamService.listTests(classId).stream()
				.map(this::toResponse)
				.toList();
		return ApiResponse.ok(list);
	}

	@PostMapping
	public ApiResponse<TestExamResponse> create(
			@PathVariable long classId,
			@Valid @RequestBody CreateTestRequest request) {
		TestExam created = testExamService.createTest(
				classId,
				request.title(),
				request.testAt(),
				request.questionCount(),
				request.retakeThresholdCount(),
				request.targetStudentIds());
		return ApiResponse.ok(toResponse(created));
	}

	@PostMapping("/{testId}/retakes")
	public ApiResponse<TestExamResponse> createRetake(
			@PathVariable long testId,
			@Valid @RequestBody CreateTestRetakeRequest request) {
		return ApiResponse.ok(toResponse(testExamService.createRetake(testId, request.testAt())));
	}

	@GetMapping("/{testId}/answer-keys")
	public ApiResponse<TestAnswerKeyResponse> getAnswerKeys(@PathVariable long testId) {
		return ApiResponse.ok(testExamService.getAnswerKeyInfo(testId));
	}

	@PostMapping(value = "/{testId}/answer-keys", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse<TestAnswerKeyResponse> uploadAnswerKey(
			@PathVariable long testId,
			@RequestPart("file") MultipartFile file,
			@RequestParam("questionCount") int questionCount) {
		TestExam test = testExamService.uploadAnswerKeyPdf(testId, questionCount, file);
		return ApiResponse.ok(TestAnswerKeyResponse.from(test));
	}

	@GetMapping("/{testId}/answer-keys/pdf")
	public ResponseEntity<Resource> downloadAnswerKeyPdf(@PathVariable long testId) {
		Resource resource = testExamService.loadAnswerKeyPdf(testId);
		String path = testExamService.getAnswerKeyRelativePath(testId);
		String filename = answerKeyStorageService.downloadFilename(path, "test-" + testId + "-answer-key");
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
				.contentType(answerKeyStorageService.mediaTypeForPath(path))
				.body(resource);
	}

	@GetMapping("/{testId}/scores")
	public ApiResponse<List<TestScoreResponse>> listScores(@PathVariable long testId) {
		TestExam test = testExamService.getTest(testId);
		List<TestScoreResponse> rows = testExamService.listScoreRows(testId).stream()
				.map(row -> TestScoreResponse.from(row, test))
				.toList();
		return ApiResponse.ok(rows);
	}

	@PatchMapping("/{testId}/scores")
	public ApiResponse<TestExamResponse> saveScores(
			@PathVariable long testId,
			@Valid @RequestBody UpdateTestScoresRequest request) {
		List<ScoreUpdate> updates = request.scores().stream()
				.map(s -> new ScoreUpdate(s.studentId(), s.rawScore(), s.grade()))
				.toList();
		return ApiResponse.ok(toResponse(testExamService.saveScores(testId, updates)));
	}

	@PatchMapping("/{testId}/scores/{studentId}/grade")
	public ApiResponse<TestScoreResponse> recordScoreResult(
			@PathVariable long testId,
			@PathVariable long studentId,
			@Valid @RequestBody RecordSubmissionResultRequest request) {
		testExamService.recordScoreResult(
				testId,
				studentId,
				request.correctCount(),
				request.wrongQuestionNos());
		TestExam test = testExamService.getTest(testId);
		var row = testExamService.listScoreRows(testId).stream()
				.filter(r -> r.studentId() == studentId)
				.findFirst()
				.orElseThrow();
		return ApiResponse.ok(TestScoreResponse.from(row, test));
	}

	@PatchMapping("/{testId}/complete")
	public ApiResponse<TestExamResponse> complete(@PathVariable long testId) {
		return ApiResponse.ok(toResponse(testExamService.completeTest(testId)));
	}

	private TestExamResponse toResponse(TestExam test) {
		long testId = test.testId();
		AssignmentStatus status = test.status();
		int pendingGradeCount = testExamService.countPendingGrades(testId);
		if (currentUserService.requireRole() == UserRole.STUDENT) {
			long me = currentUserService.requireUserId();
			boolean minePending = testExamService.isGradePendingForStudent(testId, me);
			status = minePending ? AssignmentStatus.SCHEDULED : AssignmentStatus.COMPLETED;
			pendingGradeCount = minePending ? 1 : 0;
		}
		return TestExamResponse.from(
				test,
				testExamService.getTargets(testId),
				status,
				testExamService.usesCountOnlyGrading(testId),
				pendingGradeCount);
	}
}
