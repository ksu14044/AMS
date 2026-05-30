package com.example.ams.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ams.common.BusinessException;
import com.example.ams.common.ErrorCode;
import com.example.ams.domain.clazz.AssignmentStatus;
import com.example.ams.domain.clazz.Clazz;
import com.example.ams.domain.clazz.TestExam;
import com.example.ams.domain.clazz.TestScore;
import com.example.ams.domain.user.User;
import com.example.ams.domain.user.UserRole;
import com.example.ams.event.TestExamCreatedEvent;
import com.example.ams.event.TestResultUpdatedEvent;
import com.example.ams.repository.ClassEnrollmentRepository;
import com.example.ams.repository.TestExamRepository;
import com.example.ams.repository.TestScoreRepository;
import com.example.ams.repository.UserRepository;
import com.example.ams.security.CurrentUserService;

@Service
public class TestExamService {

	private final TestExamRepository testRepository;
	private final TestScoreRepository scoreRepository;
	private final ClassEnrollmentRepository enrollmentRepository;
	private final UserRepository userRepository;
	private final ClassAccessService classAccessService;
	private final CurrentUserService currentUserService;
	private final ApplicationEventPublisher eventPublisher;

	public TestExamService(
			TestExamRepository testRepository,
			TestScoreRepository scoreRepository,
			ClassEnrollmentRepository enrollmentRepository,
			UserRepository userRepository,
			ClassAccessService classAccessService,
			CurrentUserService currentUserService,
			ApplicationEventPublisher eventPublisher) {
		this.testRepository = testRepository;
		this.scoreRepository = scoreRepository;
		this.enrollmentRepository = enrollmentRepository;
		this.userRepository = userRepository;
		this.classAccessService = classAccessService;
		this.currentUserService = currentUserService;
		this.eventPublisher = eventPublisher;
	}

	public List<TestExam> listTests(long classId) {
		classAccessService.requireReadableClass(classId);
		return testRepository.findByClassId(classId);
	}

	public TestExam getTest(long testId) {
		TestExam test = testRepository.findById(testId)
				.orElseThrow(() -> new BusinessException(ErrorCode.TEST_NOT_FOUND));
		classAccessService.requireReadableClass(test.classId());
		return test;
	}

	public List<ScoreRow> listScoreRows(long testId) {
		TestExam test = getTest(testId);
		List<Long> studentIds = enrollmentRepository.findByClassId(test.classId()).stream()
				.map(e -> e.studentId())
				.toList();
		Map<Long, TestScore> byStudent = scoreRepository.findByTestId(testId).stream()
				.collect(Collectors.toMap(TestScore::studentId, s -> s));

		List<ScoreRow> rows = new ArrayList<>();
		for (long studentId : studentIds) {
			TestScore score = byStudent.get(studentId);
			if (score == null) {
				scoreRepository.insertEmpty(testId, studentId);
				score = scoreRepository.findByTestIdAndStudentId(testId, studentId).orElseThrow();
			}
			User student = userRepository.findById(studentId).orElseThrow();
			rows.add(new ScoreRow(studentId, student.name(), score));
		}
		if (currentUserService.requireRole() == UserRole.STUDENT) {
			long me = currentUserService.requireUserId();
			return rows.stream().filter(r -> r.studentId() == me).toList();
		}
		return rows;
	}

	@Transactional
	public TestExam createTest(long classId, String title, Instant testAt) {
		Clazz clazz = classAccessService.requireReadableClass(classId);
		classAccessService.requireManageClassContent(clazz);
		return insertTest(classId, null, title, testAt);
	}

	@Transactional
	public TestExam createTestForLessonRecord(
			long classId,
			long lessonRecordId,
			String title,
			Instant testAt) {
		Clazz clazz = classAccessService.requireReadableClass(classId);
		classAccessService.requireEditClassContent(clazz);
		return insertTest(classId, lessonRecordId, title, testAt);
	}

	private TestExam insertTest(long classId, Long lessonRecordId, String title, Instant testAt) {
		TestExam test = testRepository.insert(classId, lessonRecordId, title, testAt, AssignmentStatus.SCHEDULED);
		for (var e : enrollmentRepository.findByClassId(classId)) {
			scoreRepository.insertEmpty(test.testId(), e.studentId());
		}
		eventPublisher.publishEvent(new TestExamCreatedEvent(classId, test.testId(), title));
		return test;
	}

	@Transactional
	public TestExam saveScores(long testId, List<ScoreUpdate> updates) {
		TestExam test = getTest(testId);
		Clazz clazz = classAccessService.requireReadableClass(test.classId());
		classAccessService.requireManageClassContent(clazz);

		List<Long> studentIds = enrollmentRepository.findByClassId(test.classId()).stream()
				.map(e -> e.studentId())
				.toList();
		Map<Long, ScoreUpdate> updateMap = updates.stream()
				.collect(Collectors.toMap(ScoreUpdate::studentId, u -> u));

		List<TestScoreCalculator.ScoreInput> inputs = new ArrayList<>();
		for (long studentId : studentIds) {
			ScoreUpdate u = updateMap.get(studentId);
			BigDecimal raw = u != null ? u.rawScore() : null;
			inputs.add(new TestScoreCalculator.ScoreInput(studentId, raw));
		}

		List<TestScoreCalculator.ScoreOutput> computed = TestScoreCalculator.compute(inputs);
		for (int i = 0; i < studentIds.size(); i++) {
			long studentId = studentIds.get(i);
			ScoreUpdate u = updateMap.get(studentId);
			TestScoreCalculator.ScoreOutput out = computed.get(i);
			scoreRepository.updateScore(
					testId,
					studentId,
					u != null ? u.rawScore() : null,
					u != null ? u.grade() : null,
					out.classAvgPerStudent(),
					out.upperRankPct(),
					out.percentileRank());
		}

		BigDecimal classAverage = computed.isEmpty() ? null : computed.get(0).classAverage();
		testRepository.complete(testId, classAverage, Instant.now());
		for (long studentId : studentIds) {
			ScoreUpdate update = updateMap.get(studentId);
			if (update != null && update.rawScore() != null) {
				eventPublisher.publishEvent(new TestResultUpdatedEvent(
						test.classId(), testId, studentId, test.title()));
			}
		}
		return testRepository.findById(testId).orElseThrow();
	}

	public record ScoreRow(long studentId, String studentName, TestScore score) {
	}

	public record ScoreUpdate(long studentId, BigDecimal rawScore, String grade) {
	}
}
