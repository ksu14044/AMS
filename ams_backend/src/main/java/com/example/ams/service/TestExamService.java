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
import com.example.ams.common.HomeworkAnswersJson;
import com.example.ams.domain.clazz.AssignmentEntityType;
import com.example.ams.domain.clazz.AssignmentStatus;
import com.example.ams.domain.clazz.Clazz;
import com.example.ams.domain.clazz.TestAnswerKey;
import com.example.ams.domain.clazz.TestExam;
import com.example.ams.domain.clazz.TestScore;
import com.example.ams.domain.user.User;
import com.example.ams.domain.user.UserRole;
import com.example.ams.event.TestExamCreatedEvent;
import com.example.ams.event.TestResultUpdatedEvent;
import com.example.ams.repository.ClassEnrollmentRepository;
import com.example.ams.repository.TestAnswerKeyRepository;
import com.example.ams.repository.TestExamRepository;
import com.example.ams.repository.TestScoreRepository;
import com.example.ams.repository.UserRepository;
import com.example.ams.security.CurrentUserService;

@Service
public class TestExamService {

	private static final int MAX_RETAKE_COUNT = 3;

	private final TestExamRepository testRepository;
	private final TestScoreRepository scoreRepository;
	private final TestAnswerKeyRepository answerKeyRepository;
	private final ClassEnrollmentRepository enrollmentRepository;
	private final UserRepository userRepository;
	private final ClassAccessService classAccessService;
	private final CurrentUserService currentUserService;
	private final AssignmentTargetService assignmentTargetService;
	private final ApplicationEventPublisher eventPublisher;

	public TestExamService(
			TestExamRepository testRepository,
			TestScoreRepository scoreRepository,
			TestAnswerKeyRepository answerKeyRepository,
			ClassEnrollmentRepository enrollmentRepository,
			UserRepository userRepository,
			ClassAccessService classAccessService,
			CurrentUserService currentUserService,
			AssignmentTargetService assignmentTargetService,
			ApplicationEventPublisher eventPublisher) {
		this.testRepository = testRepository;
		this.scoreRepository = scoreRepository;
		this.answerKeyRepository = answerKeyRepository;
		this.enrollmentRepository = enrollmentRepository;
		this.userRepository = userRepository;
		this.classAccessService = classAccessService;
		this.currentUserService = currentUserService;
		this.assignmentTargetService = assignmentTargetService;
		this.eventPublisher = eventPublisher;
	}

	public List<TestExam> listTests(long classId) {
		classAccessService.requireReadableClass(classId);
		List<TestExam> tests = testRepository.findByClassId(classId);
		if (currentUserService.requireRole() == UserRole.STUDENT) {
			long me = currentUserService.requireUserId();
			return tests.stream()
					.filter(t -> {
						if (t.isRetake()) {
							return scoreRepository.findByTestIdAndStudentId(t.testId(), me).isPresent();
						}
						return assignmentTargetService.canStudentAccess(
								AssignmentEntityType.TEST, t.testId(), classId, me);
					})
					.toList();
		}
		return tests;
	}

	public AssignmentTargetService.TargetView getTargets(long testId) {
		TestExam test = getTest(testId);
		if (test.isRetake()) {
			List<Long> studentIds = scoreRepository.findByTestId(testId).stream()
					.map(TestScore::studentId)
					.toList();
			return new AssignmentTargetService.TargetView(studentIds, false);
		}
		return assignmentTargetService.getTargetView(AssignmentEntityType.TEST, testId, test.classId());
	}

	public TestExam getTest(long testId) {
		TestExam test = testRepository.findById(testId)
				.orElseThrow(() -> new BusinessException(ErrorCode.TEST_NOT_FOUND));
		classAccessService.requireReadableClass(test.classId());
		return test;
	}

	public List<ScoreRow> listScoreRows(long testId) {
		TestExam test = getTest(testId);
		List<Long> studentIds = assignmentTargetService.resolveTargetStudentIds(
				AssignmentEntityType.TEST, testId, test.classId());
		Map<Long, TestScore> byStudent = scoreRepository.findByTestId(testId).stream()
				.collect(Collectors.toMap(TestScore::studentId, s -> s));

		List<ScoreRow> rows = new ArrayList<>();
		for (long studentId : studentIds) {
			TestScore score = byStudent.get(studentId);
			if (score == null) {
				if (test.isRetake()) {
					continue;
				}
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
	public TestExam createTest(
			long classId,
			String title,
			Instant testAt,
			Integer questionCount,
			Integer retakeThresholdCount,
			List<Long> targetStudentIds) {
		Clazz clazz = classAccessService.requireReadableClass(classId);
		classAccessService.requireManageClassContent(clazz);
		return insertTest(classId, null, title, testAt, questionCount, retakeThresholdCount, null, 0, targetStudentIds);
	}

	@Transactional
	public TestExam createTestForLessonRecord(
			long classId,
			long lessonRecordId,
			String title,
			Instant testAt,
			Integer questionCount,
			Integer retakeThresholdCount,
			List<Long> targetStudentIds) {
		Clazz clazz = classAccessService.requireReadableClass(classId);
		classAccessService.requireEditClassContent(clazz);
		return insertTest(
				classId,
				lessonRecordId,
				title,
				testAt,
				questionCount,
				retakeThresholdCount,
				null,
				0,
				targetStudentIds);
	}

	private TestExam insertTest(
			long classId,
			Long lessonRecordId,
			String title,
			Instant testAt,
			Integer questionCount,
			Integer retakeThresholdCount,
			Long parentTestId,
			int retakeAttemptNo,
			List<Long> targetStudentIds) {
		TestExam test = testRepository.insert(
				classId,
				lessonRecordId,
				title,
				testAt,
				AssignmentStatus.SCHEDULED,
				questionCount,
				retakeThresholdCount,
				parentTestId,
				retakeAttemptNo);
		if (!test.isRetake()) {
			assignmentTargetService.applyOnCreate(
					AssignmentEntityType.TEST, test.testId(), classId, targetStudentIds);
			for (long studentId : assignmentTargetService.resolveTargetStudentIds(
					AssignmentEntityType.TEST, test.testId(), classId)) {
				scoreRepository.insertEmpty(test.testId(), studentId);
			}
		}
		eventPublisher.publishEvent(new TestExamCreatedEvent(classId, test.testId(), title));
		return test;
	}

	@Transactional
	public TestExam createRetake(long testId, Instant testAt) {
		TestExam reference = getTest(testId);
		TestExam root = resolveRoot(reference);
		Clazz clazz = classAccessService.requireReadableClass(root.classId());
		classAccessService.requireEditClassContent(clazz);

		if (root.questionCount() == null || root.retakeThresholdCount() == null) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "문항 수·재시험 기준을 설정한 본시험만 재시험을 등록할 수 있습니다.");
		}

		List<TestExam> retakes = testRepository.findRetakesByParentTestId(root.testId());
		if (retakes.stream().anyMatch(r -> r.status() == AssignmentStatus.SCHEDULED)) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "예정된 재시험이 있습니다. 먼저 완료하세요.");
		}
		if (retakes.size() >= MAX_RETAKE_COUNT) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "재시험은 최대 " + MAX_RETAKE_COUNT + "회까지 등록할 수 있습니다.");
		}

		TestExam latestCompleted = findLatestCompletedInChain(root, retakes);
		List<Long> targets = resolveRetakeTargets(latestCompleted, root.questionCount(), root.retakeThresholdCount());
		if (targets.isEmpty()) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "재시험 대상 학생이 없습니다.");
		}

		int nextAttempt = retakes.size() + 1;
		String title = root.title() + " 재시험 " + nextAttempt;
		TestExam retake = testRepository.insert(
				root.classId(),
				null,
				title,
				testAt,
				AssignmentStatus.SCHEDULED,
				root.questionCount(),
				root.retakeThresholdCount(),
				root.testId(),
				nextAttempt);
		for (long studentId : targets) {
			scoreRepository.insertEmpty(retake.testId(), studentId);
		}
		eventPublisher.publishEvent(new TestExamCreatedEvent(root.classId(), retake.testId(), title));
		return retake;
	}

	public List<TestAnswerKey> getAnswerKeys(long testId) {
		TestExam test = getTest(testId);
		return answerKeyRepository.findByTestId(test.testId());
	}

	@Transactional
	public TestExam saveAnswerKeys(long testId, int questionCount, List<String> answers) {
		TestExam test = getTest(testId);
		classAccessService.requireEditClassContent(
				classAccessService.requireReadableClass(test.classId()));
		if (answers.size() != questionCount) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "정답 개수가 문항 수와 일치하지 않습니다.");
		}
		List<TestAnswerKey> keys = new ArrayList<>();
		for (int i = 0; i < questionCount; i++) {
			String answer = answers.get(i);
			if (answer == null || answer.isBlank()) {
				throw new BusinessException(ErrorCode.INVALID_REQUEST, (i + 1) + "번 문항 정답을 입력하세요.");
			}
			keys.add(new TestAnswerKey(testId, i + 1, answer.trim()));
		}
		answerKeyRepository.replaceAll(testId, keys);
		testRepository.updateQuestionCount(testId, questionCount);
		return testRepository.findById(testId).orElseThrow();
	}

	@Transactional
	public TestScore gradeScore(long testId, long studentId, List<String> answers) {
		TestExam test = getTest(testId);
		Clazz clazz = classAccessService.requireReadableClass(test.classId());
		classAccessService.requireEditClassContent(clazz);
		if (!enrollmentRepository.existsByClassIdAndStudentId(test.classId(), studentId)) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "수강생만 채점할 수 있습니다.");
		}
		Integer questionCount = test.questionCount();
		if (questionCount == null || questionCount <= 0) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "문항 수가 설정되지 않았습니다.");
		}
		List<TestAnswerKey> keys = answerKeyRepository.findByTestId(testId);
		if (keys.isEmpty()) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "정답지를 먼저 저장하세요.");
		}
		List<String> correctAnswers = keys.stream()
				.sorted(java.util.Comparator.comparingInt(TestAnswerKey::questionNo))
				.map(TestAnswerKey::correctAnswer)
				.toList();
		List<String> normalizedAnswers = HomeworkAnswersJson.normalizeToCount(answers, questionCount);
		int correctCount = HomeworkScoreCalculator.countCorrect(normalizedAnswers, correctAnswers);
		BigDecimal rawScore = HomeworkScoreCalculator.computeScore(questionCount, correctCount);
		var existing = scoreRepository.findByTestIdAndStudentId(testId, studentId).orElse(null);
		TestScore updated = scoreRepository.upsertGraded(
				testId,
				studentId,
				normalizedAnswers,
				correctCount,
				rawScore,
				Instant.now());
		if (test.status() == AssignmentStatus.COMPLETED) {
			refreshRanksAndClassAverage(testId);
			updated = scoreRepository.findByTestIdAndStudentId(testId, studentId).orElseThrow();
		}
		if (shouldNotifyGradedResult(existing, rawScore)) {
			eventPublisher.publishEvent(new TestResultUpdatedEvent(
					test.classId(), testId, studentId, test.title()));
		}
		return updated;
	}

	@Transactional
	public TestExam completeTest(long testId) {
		TestExam test = getTest(testId);
		classAccessService.requireEditClassContent(
				classAccessService.requireReadableClass(test.classId()));
		if (test.status() == AssignmentStatus.COMPLETED) {
			return test;
		}
		List<TestScore> scores = scoreRepository.findByTestId(testId);
		boolean anyGraded = scores.stream().anyMatch(s -> s.rawScore() != null);
		if (!anyGraded) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "채점된 학생이 없습니다.");
		}
		BigDecimal classAverage = refreshRanksAndClassAverage(testId);
		testRepository.complete(testId, classAverage, Instant.now());
		return testRepository.findById(testId).orElseThrow();
	}

	@Transactional
	public TestExam saveScores(long testId, List<ScoreUpdate> updates) {
		TestExam test = getTest(testId);
		Clazz clazz = classAccessService.requireReadableClass(test.classId());
		classAccessService.requireEditClassContent(clazz);

		List<Long> studentIds = scoreRepository.findByTestId(testId).stream()
				.map(TestScore::studentId)
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
					out.rank());
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

	private BigDecimal refreshRanksAndClassAverage(long testId) {
		BigDecimal classAverage = recalculateRanks(testId);
		testRepository.updateClassAverage(testId, classAverage);
		return classAverage;
	}

	private BigDecimal recalculateRanks(long testId) {
		List<TestScore> scores = scoreRepository.findByTestId(testId);
		List<TestScoreCalculator.ScoreInput> inputs = scores.stream()
				.map(s -> new TestScoreCalculator.ScoreInput(s.studentId(), s.rawScore()))
				.toList();
		List<TestScoreCalculator.ScoreOutput> computed = TestScoreCalculator.compute(inputs);
		for (int i = 0; i < scores.size(); i++) {
			TestScore score = scores.get(i);
			TestScoreCalculator.ScoreOutput out = computed.get(i);
			scoreRepository.updateRankAndClassAvg(
					testId,
					score.studentId(),
					out.classAvgPerStudent(),
					out.rank());
		}
		return computed.isEmpty() ? null : computed.get(0).classAverage();
	}

	private static boolean shouldNotifyGradedResult(TestScore before, BigDecimal rawScore) {
		if (rawScore == null) {
			return false;
		}
		if (before == null || before.rawScore() == null) {
			return true;
		}
		return before.rawScore().compareTo(rawScore) != 0;
	}

	private TestExam resolveRoot(TestExam test) {
		if (!test.isRetake()) {
			return test;
		}
		return testRepository.findById(test.rootTestId()).orElseThrow();
	}

	private TestExam findLatestCompletedInChain(TestExam root, List<TestExam> retakes) {
		TestExam latest = root.status() == AssignmentStatus.COMPLETED ? root : null;
		for (TestExam retake : retakes) {
			if (retake.status() == AssignmentStatus.COMPLETED) {
				if (latest == null || retake.retakeAttemptNo() > latest.retakeAttemptNo()) {
					latest = retake;
				}
			}
		}
		if (latest == null) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "완료된 시험이 없습니다. 점수를 먼저 저장하세요.");
		}
		return latest;
	}

	private List<Long> resolveRetakeTargets(TestExam latestCompleted, int questionCount, int retakeThresholdCount) {
		List<Long> targets = new ArrayList<>();
		for (TestScore score : scoreRepository.findByTestId(latestCompleted.testId())) {
			if (TestRetakeEvaluator.needsRetake(score.rawScore(), questionCount, retakeThresholdCount)) {
				targets.add(score.studentId());
			}
		}
		return targets;
	}

	public record ScoreRow(long studentId, String studentName, TestScore score) {
	}

	public record ScoreUpdate(long studentId, BigDecimal rawScore, String grade) {
	}
}
