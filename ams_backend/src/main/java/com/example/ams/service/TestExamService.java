package com.example.ams.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.ams.api.dto.TestAnswerKeyResponse;
import com.example.ams.common.BusinessException;
import com.example.ams.common.ErrorCode;
import com.example.ams.domain.clazz.AssignmentEntityType;
import com.example.ams.domain.clazz.AssignmentStatus;
import com.example.ams.domain.clazz.Clazz;
import com.example.ams.domain.clazz.TestExam;
import com.example.ams.domain.clazz.TestScore;
import com.example.ams.domain.user.User;
import com.example.ams.domain.user.UserRole;
import com.example.ams.event.TestExamCreatedEvent;
import com.example.ams.event.TestResultUpdatedEvent;
import com.example.ams.repository.ClassEnrollmentRepository;
import com.example.ams.repository.LessonRecordRepository;
import com.example.ams.repository.TestExamRepository;
import com.example.ams.repository.TestScoreRepository;
import com.example.ams.repository.UserRepository;
import com.example.ams.security.CurrentUserService;

@Service
public class TestExamService {

	private static final int MAX_RETAKE_COUNT = 3;

	private final TestExamRepository testRepository;
	private final TestScoreRepository scoreRepository;
	private final LessonRecordRepository lessonRecordRepository;
	private final ClassEnrollmentRepository enrollmentRepository;
	private final UserRepository userRepository;
	private final ClassAccessService classAccessService;
	private final CurrentUserService currentUserService;
	private final AssignmentTargetService assignmentTargetService;
	private final AnswerKeyPdfStorageService answerKeyPdfStorageService;
	private final ApplicationEventPublisher eventPublisher;

	public TestExamService(
			TestExamRepository testRepository,
			TestScoreRepository scoreRepository,
			LessonRecordRepository lessonRecordRepository,
			ClassEnrollmentRepository enrollmentRepository,
			UserRepository userRepository,
			ClassAccessService classAccessService,
			CurrentUserService currentUserService,
			AssignmentTargetService assignmentTargetService,
			AnswerKeyPdfStorageService answerKeyPdfStorageService,
			ApplicationEventPublisher eventPublisher) {
		this.testRepository = testRepository;
		this.scoreRepository = scoreRepository;
		this.lessonRecordRepository = lessonRecordRepository;
		this.enrollmentRepository = enrollmentRepository;
		this.userRepository = userRepository;
		this.classAccessService = classAccessService;
		this.currentUserService = currentUserService;
		this.assignmentTargetService = assignmentTargetService;
		this.answerKeyPdfStorageService = answerKeyPdfStorageService;
		this.eventPublisher = eventPublisher;
	}

	public List<TestExam> listTests(long classId) {
		classAccessService.requireReadableClass(classId);
		List<TestExam> tests = testRepository.findByClassId(classId);
		if (currentUserService.requireRole() == UserRole.STUDENT) {
			long me = currentUserService.requireUserId();
			LocalDate accessibleFrom = enrollmentRepository.findByClassIdAndStudentId(classId, me)
					.map(e -> e.accessibleFrom())
					.orElse(null);
			Map<Long, LocalDate> lessonDateCache = new HashMap<>();
			return tests.stream()
					.filter(t -> {
						if (t.isRetake()) {
							return scoreRepository.findByTestIdAndStudentId(t.testId(), me).isPresent();
						}
						return assignmentTargetService.canStudentAccess(
								AssignmentEntityType.TEST, t.testId(), classId, me);
					})
					.filter(t -> isVisibleWithinAccessWindow(t, accessibleFrom, lessonDateCache))
					.toList();
		}
		return tests;
	}

	private boolean isVisibleWithinAccessWindow(
			TestExam test,
			LocalDate accessibleFrom,
			Map<Long, LocalDate> lessonDateCache) {
		if (accessibleFrom == null || test.isRetake()) {
			return true;
		}
		Long lessonRecordId = testRepository.findLessonRecordId(test.testId());
		if (lessonRecordId == null) {
			return true;
		}
		LocalDate lessonDate = lessonDateCache.computeIfAbsent(
				lessonRecordId,
				id -> lessonRecordRepository.findById(id).map(r -> r.lessonDate()).orElse(null));
		if (lessonDate == null || !lessonDate.isBefore(accessibleFrom)) {
			return true;
		}
		return false;
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

	@Transactional
	public TestExam uploadAnswerKeyPdf(long testId, int questionCount, MultipartFile file) {
		TestExam test = getTest(testId);
		if (test.isRetake()) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "재시험에는 정답지를 등록할 수 없습니다.");
		}
		Clazz clazz = classAccessService.requireReadableClass(test.classId());
		classAccessService.requireEditClassContent(clazz);
		if (questionCount <= 0) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "문항 수를 1 이상 입력하세요.");
		}
		String path = answerKeyPdfStorageService.storeTestAnswerKey(
				clazz.academyId(), test.classId(), testId, file);
		answerKeyPdfStorageService.deleteIfExists(test.answerKeyPdfPath());
		testRepository.updateAnswerKeyPdfPath(testId, path);
		testRepository.updateQuestionCount(testId, questionCount);
		return testRepository.findById(testId).orElseThrow();
	}

	public Resource loadAnswerKeyPdf(long testId) {
		TestExam test = getTest(testId);
		classAccessService.requireReadableClass(test.classId());
		TestExam source = resolveAnswerKeySource(test);
		return answerKeyPdfStorageService.load(source.answerKeyPdfPath());
	}

	public String getAnswerKeyRelativePath(long testId) {
		TestExam test = getTest(testId);
		return resolveAnswerKeySource(test).answerKeyPdfPath();
	}

	public boolean usesCountOnlyGrading(long testId) {
		TestExam test = getTest(testId);
		long lessonLookupTestId = test.isRetake() ? test.rootTestId() : test.testId();
		Long lessonRecordId = testRepository.findLessonRecordId(lessonLookupTestId);
		return lessonRecordId != null;
	}

	private TestExam resolveAnswerKeySource(TestExam test) {
		if (!test.isRetake()) {
			return test;
		}
		TestExam root = testRepository.findById(test.rootTestId()).orElseThrow();
		if (root.answerKeyPdfPath() == null || root.answerKeyPdfPath().isBlank()) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "본시험 정답지가 등록되지 않았습니다.");
		}
		return root;
	}

	@Transactional
	public TestScore recordScoreResult(
			long testId,
			long studentId,
			Integer correctCount,
			List<Integer> wrongQuestionNos) {
		TestExam test = getTest(testId);
		Clazz clazz = classAccessService.requireReadableClass(test.classId());
		classAccessService.requireEditClassContent(clazz);
		if (!enrollmentRepository.existsByClassIdAndStudentId(test.classId(), studentId)) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "수강생만 결과를 입력할 수 있습니다.");
		}
		Integer questionCount = test.questionCount();
		if (questionCount == null || questionCount <= 0) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "문항 수가 설정되지 않았습니다.");
		}
		boolean countOnly = usesCountOnlyGrading(test.testId());
		SubmissionResultValidator.Result result;
		if (countOnly) {
			if (correctCount == null) {
				throw new BusinessException(ErrorCode.INVALID_REQUEST, "맞은 문항 수를 입력하세요.");
			}
			result = SubmissionResultValidator.fromCorrectCount(questionCount, correctCount);
		} else {
			result = SubmissionResultValidator.fromWrongQuestions(questionCount, wrongQuestionNos);
		}
		int resolvedCorrectCount = result.correctCount();
		List<Integer> normalizedWrong = result.wrongQuestionNos();
		BigDecimal rawScore = HomeworkScoreCalculator.computeScore(questionCount, resolvedCorrectCount);
		var existing = scoreRepository.findByTestIdAndStudentId(testId, studentId).orElse(null);
		TestScore updated = scoreRepository.upsertGraded(
				testId,
				studentId,
				resolvedCorrectCount,
				normalizedWrong,
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
		maybeAutoCompleteTest(testId);
		return updated;
	}

	public boolean isGradePendingForStudent(long testId, long studentId) {
		TestScore score = scoreRepository.findByTestIdAndStudentId(testId, studentId).orElse(null);
		return score == null || score.rawScore() == null;
	}

	public int countPendingGrades(long testId) {
		TestExam test = getTest(testId);
		List<Long> studentIds = assignmentTargetService.resolveTargetStudentIds(
				AssignmentEntityType.TEST, testId, test.classId());
		if (studentIds.isEmpty()) {
			return 0;
		}
		Map<Long, TestScore> byStudent = scoreRepository.findByTestId(testId).stream()
				.collect(Collectors.toMap(TestScore::studentId, s -> s));
		int pending = 0;
		for (long studentId : studentIds) {
			TestScore score = byStudent.get(studentId);
			if (score == null || score.rawScore() == null) {
				pending++;
			}
		}
		return pending;
	}

	private void maybeAutoCompleteTest(long testId) {
		TestExam test = testRepository.findById(testId).orElseThrow();
		if (test.status() == AssignmentStatus.COMPLETED) {
			refreshRanksAndClassAverage(testId);
			return;
		}
		List<Long> studentIds = assignmentTargetService.resolveTargetStudentIds(
				AssignmentEntityType.TEST, testId, test.classId());
		if (studentIds.isEmpty() || countPendingGrades(testId) > 0) {
			return;
		}
		BigDecimal classAverage = refreshRanksAndClassAverage(testId);
		testRepository.complete(testId, classAverage, Instant.now());
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

	public TestAnswerKeyResponse getAnswerKeyInfo(long testId) {
		TestExam test = getTest(testId);
		TestExam source = test;
		if (test.isRetake()) {
			source = testRepository.findById(test.rootTestId()).orElseThrow();
		}
		int count = test.questionCount() != null ? test.questionCount() : 0;
		boolean hasFile = source.answerKeyPdfPath() != null && !source.answerKeyPdfPath().isBlank();
		return new TestAnswerKeyResponse(count, hasFile);
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

	@Transactional
	public TestExam updateTestMetadata(
			long testId,
			String title,
			Integer questionCount,
			Integer retakeThresholdCount,
			List<Long> targetStudentIds) {
		TestExam test = getTest(testId);
		classAccessService.requireEditClassContent(
				classAccessService.requireReadableClass(test.classId()));
		if (test.isRetake()) {
			throw new BusinessException(ErrorCode.LESSON_RECORD_LINK_LOCKED, "재시험은 수업기록에서 수정할 수 없습니다.");
		}
		if (test.status() == AssignmentStatus.SCHEDULED) {
			testRepository.updateMetadata(testId, title.trim(), questionCount, retakeThresholdCount);
			assignmentTargetService.updateTargets(
					AssignmentEntityType.TEST, testId, test.classId(), targetStudentIds);
			syncScoreRows(testId, test.classId());
		} else {
			if (!title.trim().equals(test.title())) {
				throw new BusinessException(ErrorCode.LESSON_RECORD_LINK_LOCKED, "완료된 테스트는 제목·문항 수를 변경할 수 없습니다.");
			}
			assignmentTargetService.updateTargets(
					AssignmentEntityType.TEST, testId, test.classId(), targetStudentIds);
		}
		return testRepository.findById(testId).orElseThrow();
	}

	@Transactional
	public void deleteTestIfAllowed(long testId) {
		TestExam test = getTest(testId);
		classAccessService.requireEditClassContent(
				classAccessService.requireReadableClass(test.classId()));
		if (test.isRetake()) {
			throw new BusinessException(ErrorCode.LESSON_RECORD_LINK_LOCKED, "재시험은 삭제할 수 없습니다.");
		}
		if (test.status() != AssignmentStatus.SCHEDULED) {
			throw new BusinessException(ErrorCode.LESSON_RECORD_LINK_LOCKED, "완료된 테스트는 삭제할 수 없습니다.");
		}
		if (scoreRepository.hasGradedScore(testId)) {
			throw new BusinessException(ErrorCode.LESSON_RECORD_LINK_LOCKED, "채점된 테스트는 삭제할 수 없습니다.");
		}
		if (!testRepository.findRetakesByParentTestId(testId).isEmpty()) {
			throw new BusinessException(ErrorCode.LESSON_RECORD_LINK_LOCKED, "재시험이 연결된 테스트는 삭제할 수 없습니다.");
		}
		assignmentTargetService.clearTargets(AssignmentEntityType.TEST, testId);
		scoreRepository.deleteByTestId(testId);
		answerKeyPdfStorageService.deleteIfExists(test.answerKeyPdfPath());
		testRepository.deleteById(testId);
	}

	private void syncScoreRows(long testId, long classId) {
		for (long studentId : assignmentTargetService.resolveTargetStudentIds(
				AssignmentEntityType.TEST, testId, classId)) {
			if (scoreRepository.findByTestIdAndStudentId(testId, studentId).isEmpty()) {
				scoreRepository.insertEmpty(testId, studentId);
			}
		}
	}

	public record ScoreRow(long studentId, String studentName, TestScore score) {
	}

	public record ScoreUpdate(long studentId, BigDecimal rawScore, String grade) {
	}
}
