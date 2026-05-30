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

import com.example.ams.api.dto.CreateTestRequest;
import com.example.ams.api.dto.CreateTestRetakeRequest;
import com.example.ams.api.dto.GradeHomeworkSubmissionRequest;
import com.example.ams.api.dto.SaveHomeworkAnswerKeyRequest;
import com.example.ams.api.dto.TestAnswerKeyResponse;
import com.example.ams.api.dto.TestExamResponse;
import com.example.ams.api.dto.TestScoreResponse;
import com.example.ams.api.dto.UpdateTestScoresRequest;
import com.example.ams.common.ApiResponse;
import com.example.ams.service.TestExamService;
import com.example.ams.service.TestExamService.ScoreUpdate;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/classes/{classId}/tests")
public class ClassTestController {

	private final TestExamService testExamService;

	public ClassTestController(TestExamService testExamService) {
		this.testExamService = testExamService;
	}

	@GetMapping
	public ApiResponse<List<TestExamResponse>> list(@PathVariable long classId) {
		List<TestExamResponse> list = testExamService.listTests(classId).stream()
				.map(TestExamResponse::from)
				.toList();
		return ApiResponse.ok(list);
	}

	@PostMapping
	public ApiResponse<TestExamResponse> create(
			@PathVariable long classId,
			@Valid @RequestBody CreateTestRequest request) {
		return ApiResponse.ok(TestExamResponse.from(
				testExamService.createTest(
						classId,
						request.title(),
						request.testAt(),
						request.questionCount(),
						request.retakeThresholdCount())));
	}

	@PostMapping("/{testId}/retakes")
	public ApiResponse<TestExamResponse> createRetake(
			@PathVariable long testId,
			@Valid @RequestBody CreateTestRetakeRequest request) {
		return ApiResponse.ok(TestExamResponse.from(
				testExamService.createRetake(testId, request.testAt())));
	}

	@GetMapping("/{testId}/answer-keys")
	public ApiResponse<TestAnswerKeyResponse> getAnswerKeys(@PathVariable long testId) {
		var test = testExamService.getTest(testId);
		return ApiResponse.ok(TestAnswerKeyResponse.from(
				test, testExamService.getAnswerKeys(testId)));
	}

	@PutMapping("/{testId}/answer-keys")
	public ApiResponse<TestAnswerKeyResponse> saveAnswerKeys(
			@PathVariable long testId,
			@Valid @RequestBody SaveHomeworkAnswerKeyRequest request) {
		var test = testExamService.saveAnswerKeys(
				testId, request.questionCount(), request.answers());
		return ApiResponse.ok(TestAnswerKeyResponse.from(
				test, testExamService.getAnswerKeys(testId)));
	}

	@GetMapping("/{testId}/scores")
	public ApiResponse<List<TestScoreResponse>> listScores(@PathVariable long testId) {
		var test = testExamService.getTest(testId);
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
		return ApiResponse.ok(TestExamResponse.from(testExamService.saveScores(testId, updates)));
	}

	@PatchMapping("/{testId}/scores/{studentId}/grade")
	public ApiResponse<TestScoreResponse> gradeScore(
			@PathVariable long testId,
			@PathVariable long studentId,
			@Valid @RequestBody GradeHomeworkSubmissionRequest request) {
		var test = testExamService.getTest(testId);
		testExamService.gradeScore(testId, studentId, request.answers());
		test = testExamService.getTest(testId);
		var row = testExamService.listScoreRows(testId).stream()
				.filter(r -> r.studentId() == studentId)
				.findFirst()
				.orElseThrow();
		return ApiResponse.ok(TestScoreResponse.from(row, test));
	}

	@PatchMapping("/{testId}/complete")
	public ApiResponse<TestExamResponse> complete(@PathVariable long testId) {
		return ApiResponse.ok(TestExamResponse.from(testExamService.completeTest(testId)));
	}
}
