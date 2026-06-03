package com.example.ams.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
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

import com.example.ams.common.BusinessException;
import com.example.ams.common.ErrorCode;
import com.example.ams.domain.clazz.AssignmentStatus;
import com.example.ams.domain.clazz.Clazz;
import com.example.ams.domain.clazz.AssignmentEntityType;
import com.example.ams.domain.clazz.Homework;
import com.example.ams.domain.clazz.HomeworkSubmission;
import com.example.ams.domain.user.User;
import com.example.ams.domain.user.UserRole;
import com.example.ams.event.HomeworkCreatedEvent;
import com.example.ams.event.HomeworkResultUpdatedEvent;
import com.example.ams.repository.ClassEnrollmentRepository;
import com.example.ams.repository.HomeworkRepository;
import com.example.ams.repository.HomeworkSubmissionRepository;
import com.example.ams.repository.LessonRecordRepository;
import com.example.ams.repository.UserRepository;
import com.example.ams.security.CurrentUserService;

@Service
public class HomeworkService {

	private final HomeworkRepository homeworkRepository;
	private final HomeworkSubmissionRepository submissionRepository;
	private final LessonRecordRepository lessonRecordRepository;
	private final ClassEnrollmentRepository enrollmentRepository;
	private final UserRepository userRepository;
	private final ClassAccessService classAccessService;
	private final CurrentUserService currentUserService;
	private final AssignmentTargetService assignmentTargetService;
	private final AnswerKeyPdfStorageService answerKeyPdfStorageService;
	private final ApplicationEventPublisher eventPublisher;

	public HomeworkService(
			HomeworkRepository homeworkRepository,
			HomeworkSubmissionRepository submissionRepository,
			LessonRecordRepository lessonRecordRepository,
			ClassEnrollmentRepository enrollmentRepository,
			UserRepository userRepository,
			ClassAccessService classAccessService,
			CurrentUserService currentUserService,
			AssignmentTargetService assignmentTargetService,
			AnswerKeyPdfStorageService answerKeyPdfStorageService,
			ApplicationEventPublisher eventPublisher) {
		this.homeworkRepository = homeworkRepository;
		this.submissionRepository = submissionRepository;
		this.lessonRecordRepository = lessonRecordRepository;
		this.enrollmentRepository = enrollmentRepository;
		this.userRepository = userRepository;
		this.classAccessService = classAccessService;
		this.currentUserService = currentUserService;
		this.assignmentTargetService = assignmentTargetService;
		this.answerKeyPdfStorageService = answerKeyPdfStorageService;
		this.eventPublisher = eventPublisher;
	}

	public List<Homework> listHomeworks(long classId) {
		classAccessService.requireReadableClass(classId);
		List<Homework> homeworks = homeworkRepository.findByClassId(classId);
		if (currentUserService.requireRole() == UserRole.STUDENT) {
			long me = currentUserService.requireUserId();
			LocalDate accessibleFrom = enrollmentRepository.findByClassIdAndStudentId(classId, me)
					.map(e -> e.accessibleFrom())
					.orElse(null);
			Map<Long, LocalDate> lessonDateCache = new HashMap<>();
			return homeworks.stream()
					.filter(h -> assignmentTargetService.canStudentAccess(
							AssignmentEntityType.HOMEWORK, h.homeworkId(), classId, me))
					.filter(h -> isVisibleWithinAccessWindow(h, accessibleFrom, lessonDateCache))
					.toList();
		}
		return homeworks;
	}

	private boolean isVisibleWithinAccessWindow(
			Homework homework,
			LocalDate accessibleFrom,
			Map<Long, LocalDate> lessonDateCache) {
		if (accessibleFrom == null) {
			return true;
		}
		Long lessonRecordId = homeworkRepository.findLessonRecordId(homework.homeworkId());
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

	public AssignmentTargetService.TargetView getTargets(long homeworkId) {
		Homework homework = getHomework(homeworkId);
		return assignmentTargetService.getTargetView(
				AssignmentEntityType.HOMEWORK, homeworkId, homework.classId());
	}

	public Homework getHomework(long homeworkId) {
		Homework homework = homeworkRepository.findById(homeworkId)
				.orElseThrow(() -> new BusinessException(ErrorCode.HOMEWORK_NOT_FOUND));
		classAccessService.requireReadableClass(homework.classId());
		return homework;
	}

	@Transactional
	public Homework uploadAnswerKeyPdf(long homeworkId, int questionCount, MultipartFile file) {
		Homework homework = getHomework(homeworkId);
		Clazz clazz = classAccessService.requireReadableClass(homework.classId());
		classAccessService.requireEditClassContent(clazz);
		if (questionCount <= 0) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "문항 수를 1 이상 입력하세요.");
		}
		String path = answerKeyPdfStorageService.storeHomeworkAnswerKey(
				clazz.academyId(), homework.classId(), homeworkId, file);
		answerKeyPdfStorageService.deleteIfExists(homework.answerKeyPdfPath());
		homeworkRepository.updateAnswerKeyPdfPath(homeworkId, path);
		homeworkRepository.updateQuestionCount(homeworkId, questionCount);
		return homeworkRepository.findById(homeworkId).orElseThrow();
	}

	public Resource loadAnswerKeyPdf(long homeworkId) {
		Homework homework = getHomework(homeworkId);
		classAccessService.requireReadableClass(homework.classId());
		return answerKeyPdfStorageService.load(homework.answerKeyPdfPath());
	}

	@Transactional
	public HomeworkSubmission recordSubmissionResult(
			long homeworkId,
			long studentId,
			List<Integer> wrongQuestionNos) {
		Homework homework = getHomework(homeworkId);
		Clazz clazz = classAccessService.requireReadableClass(homework.classId());
		classAccessService.requireEditClassContent(clazz);
		if (homework.answerKeyPdfPath() == null || homework.answerKeyPdfPath().isBlank()) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "정답지를 먼저 업로드하세요.");
		}
		if (!enrollmentRepository.existsByClassIdAndStudentId(homework.classId(), studentId)) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "수강생만 결과를 입력할 수 있습니다.");
		}
		Integer questionCount = homework.questionCount();
		if (questionCount == null || questionCount <= 0) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "문항 수가 설정되지 않았습니다.");
		}
		SubmissionResultValidator.Result result = SubmissionResultValidator.fromWrongQuestions(
				questionCount, wrongQuestionNos);
		int correctCount = result.correctCount();
		List<Integer> normalizedWrong = result.wrongQuestionNos();
		BigDecimal score = HomeworkScoreCalculator.computeScore(questionCount, correctCount);
		var existing = submissionRepository.findByHomeworkIdAndStudentId(homeworkId, studentId).orElse(null);
		HomeworkSubmission updated = submissionRepository.upsertGraded(
				homeworkId,
				studentId,
				correctCount,
				normalizedWrong,
				score,
				Instant.now());
		if (shouldNotifyGradedResult(existing, score)) {
			eventPublisher.publishEvent(new HomeworkResultUpdatedEvent(
					homework.classId(), homeworkId, studentId, homework.title()));
		}
		return updated;
	}

	public List<SubmissionRow> listSubmissionRows(long homeworkId) {
		Homework homework = getHomework(homeworkId);
		long classId = homework.classId();
		List<Long> studentIds = assignmentTargetService.resolveTargetStudentIds(
				AssignmentEntityType.HOMEWORK, homeworkId, classId);
		Map<Long, HomeworkSubmission> byStudent = submissionRepository.findByHomeworkId(homeworkId).stream()
				.collect(Collectors.toMap(HomeworkSubmission::studentId, s -> s));

		List<SubmissionRow> rows = new ArrayList<>();
		for (long studentId : studentIds) {
			HomeworkSubmission sub = byStudent.get(studentId);
			if (sub == null) {
				sub = submissionRepository.insertEmpty(homeworkId, studentId);
			}
			User student = userRepository.findById(studentId).orElseThrow();
			rows.add(new SubmissionRow(studentId, student.name(), sub));
		}
		if (currentUserService.requireRole() == UserRole.STUDENT) {
			long me = currentUserService.requireUserId();
			return rows.stream().filter(r -> r.studentId() == me).toList();
		}
		return rows;
	}

	@Transactional
	public Homework createHomework(long classId, String title, Integer questionCount, List<Long> targetStudentIds) {
		Clazz clazz = classAccessService.requireReadableClass(classId);
		classAccessService.requireManageClassContent(clazz);
		return insertHomework(classId, null, title, questionCount, targetStudentIds);
	}

	@Transactional
	public Homework createHomeworkForLessonRecord(
			long classId,
			long lessonRecordId,
			String title,
			Integer questionCount,
			List<Long> targetStudentIds) {
		Clazz clazz = classAccessService.requireReadableClass(classId);
		classAccessService.requireEditClassContent(clazz);
		return insertHomework(classId, lessonRecordId, title, questionCount, targetStudentIds);
	}

	private Homework insertHomework(
			long classId,
			Long lessonRecordId,
			String title,
			Integer questionCount,
			List<Long> targetStudentIds) {
		Homework homework = homeworkRepository.insert(
				classId,
				lessonRecordId,
				title,
				questionCount,
				AssignmentStatus.SCHEDULED);
		assignmentTargetService.applyOnCreate(
				AssignmentEntityType.HOMEWORK,
				homework.homeworkId(),
				classId,
				targetStudentIds);
		for (long studentId : assignmentTargetService.resolveTargetStudentIds(
				AssignmentEntityType.HOMEWORK, homework.homeworkId(), classId)) {
			submissionRepository.insertEmpty(homework.homeworkId(), studentId);
		}
		eventPublisher.publishEvent(new HomeworkCreatedEvent(classId, homework.homeworkId(), title));
		return homework;
	}

	@Transactional
	public Homework saveTargets(long homeworkId, List<Long> studentIds) {
		Homework homework = getHomework(homeworkId);
		classAccessService.requireEditClassContent(
				classAccessService.requireReadableClass(homework.classId()));
		if (studentIds.isEmpty()) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "대상 학생을 1명 이상 선택하세요.");
		}
		assignmentTargetService.saveExplicitTargets(
				AssignmentEntityType.HOMEWORK, homeworkId, homework.classId(), studentIds);
		for (long studentId : studentIds) {
			if (submissionRepository.findByHomeworkIdAndStudentId(homeworkId, studentId).isEmpty()) {
				submissionRepository.insertEmpty(homeworkId, studentId);
			}
		}
		return homeworkRepository.findById(homeworkId).orElseThrow();
	}

	private static boolean shouldNotifyGradedResult(HomeworkSubmission before, BigDecimal score) {
		if (score == null) {
			return false;
		}
		if (before == null || before.score() == null) {
			return true;
		}
		return before.score().compareTo(score) != 0;
	}

	@Transactional
	public HomeworkSubmission updateSubmission(
			long homeworkId,
			long studentId,
			boolean submitted,
			Instant submittedAt,
			BigDecimal score,
			String grade,
			String memo) {
		Homework homework = getHomework(homeworkId);
		classAccessService.requireManageClassContent(
				classAccessService.requireReadableClass(homework.classId()));
		if (!enrollmentRepository.existsByClassIdAndStudentId(homework.classId(), studentId)) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "수강생만 결과를 입력할 수 있습니다.");
		}
		var existing = submissionRepository.findByHomeworkIdAndStudentId(homeworkId, studentId).orElse(null);
		HomeworkSubmission updated = submissionRepository.upsertLegacy(
				homeworkId,
				studentId,
				submitted,
				submitted ? (submittedAt != null ? submittedAt : Instant.now()) : null,
				score,
				grade,
				memo);
		if (shouldNotifyHomeworkResult(existing, submitted, score, grade)) {
			eventPublisher.publishEvent(new HomeworkResultUpdatedEvent(
					homework.classId(), homeworkId, studentId, homework.title()));
		}
		return updated;
	}

	private static boolean shouldNotifyHomeworkResult(
			HomeworkSubmission before,
			boolean submitted,
			BigDecimal score,
			String grade) {
		boolean hasResult = submitted || score != null || hasText(grade);
		if (!hasResult) {
			return false;
		}
		if (before == null) {
			return true;
		}
		if (before.submitted() != submitted) {
			return true;
		}
		if (before.score() == null && score != null) {
			return true;
		}
		if (!hasText(before.grade()) && hasText(grade)) {
			return true;
		}
		if (before.score() != null && score != null && before.score().compareTo(score) != 0) {
			return true;
		}
		if (hasText(before.grade()) && hasText(grade) && !before.grade().equals(grade)) {
			return true;
		}
		return false;
	}

	private static boolean hasText(String value) {
		return value != null && !value.isBlank();
	}

	@Transactional
	public Homework markCompleted(long homeworkId) {
		Homework homework = getHomework(homeworkId);
		classAccessService.requireManageClassContent(
				classAccessService.requireReadableClass(homework.classId()));
		homeworkRepository.updateStatus(homeworkId, AssignmentStatus.COMPLETED);
		return homeworkRepository.findById(homeworkId).orElseThrow();
	}

	@Transactional
	public Homework updateHomeworkMetadata(
			long homeworkId,
			String title,
			Integer questionCount,
			List<Long> targetStudentIds) {
		Homework homework = getHomework(homeworkId);
		classAccessService.requireEditClassContent(
				classAccessService.requireReadableClass(homework.classId()));
		if (homework.status() == AssignmentStatus.SCHEDULED) {
			homeworkRepository.updateMetadata(homeworkId, title.trim(), questionCount);
		} else if (!title.trim().equals(homework.title())) {
			throw new BusinessException(ErrorCode.LESSON_RECORD_LINK_LOCKED, "완료된 숙제는 제목·문항 수를 변경할 수 없습니다.");
		}
		assignmentTargetService.updateTargets(
				AssignmentEntityType.HOMEWORK, homeworkId, homework.classId(), targetStudentIds);
		syncSubmissionRows(homeworkId, homework.classId());
		return homeworkRepository.findById(homeworkId).orElseThrow();
	}

	@Transactional
	public void deleteHomeworkIfAllowed(long homeworkId) {
		Homework homework = getHomework(homeworkId);
		classAccessService.requireEditClassContent(
				classAccessService.requireReadableClass(homework.classId()));
		if (homework.status() != AssignmentStatus.SCHEDULED) {
			throw new BusinessException(ErrorCode.LESSON_RECORD_LINK_LOCKED, "완료된 숙제는 삭제할 수 없습니다.");
		}
		if (submissionRepository.hasGradedSubmission(homeworkId)) {
			throw new BusinessException(ErrorCode.LESSON_RECORD_LINK_LOCKED, "채점된 숙제는 삭제할 수 없습니다.");
		}
		assignmentTargetService.clearTargets(AssignmentEntityType.HOMEWORK, homeworkId);
		submissionRepository.deleteByHomeworkId(homeworkId);
		answerKeyPdfStorageService.deleteIfExists(homework.answerKeyPdfPath());
		homeworkRepository.deleteById(homeworkId);
	}

	private void syncSubmissionRows(long homeworkId, long classId) {
		for (long studentId : assignmentTargetService.resolveTargetStudentIds(
				AssignmentEntityType.HOMEWORK, homeworkId, classId)) {
			if (submissionRepository.findByHomeworkIdAndStudentId(homeworkId, studentId).isEmpty()) {
				submissionRepository.insertEmpty(homeworkId, studentId);
			}
		}
	}

	public record SubmissionRow(long studentId, String studentName, HomeworkSubmission submission) {
	}
}
