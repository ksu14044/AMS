package com.example.ams.api.clazz;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ams.api.dto.CreateTestRequest;
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
				testExamService.createTest(classId, request.title(), request.testAt())));
	}

	@GetMapping("/{testId}/scores")
	public ApiResponse<List<TestScoreResponse>> listScores(@PathVariable long testId) {
		List<TestScoreResponse> rows = testExamService.listScoreRows(testId).stream()
				.map(TestScoreResponse::from)
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
}
