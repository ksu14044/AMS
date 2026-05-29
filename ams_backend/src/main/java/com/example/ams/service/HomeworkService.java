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
import com.example.ams.domain.clazz.Homework;
import com.example.ams.domain.clazz.HomeworkSubmission;
import com.example.ams.domain.user.User;
import com.example.ams.domain.user.UserRole;
import com.example.ams.event.HomeworkCreatedEvent;
import com.example.ams.event.HomeworkResultUpdatedEvent;
import com.example.ams.repository.ClassEnrollmentRepository;
import com.example.ams.repository.HomeworkRepository;
import com.example.ams.repository.HomeworkSubmissionRepository;
import com.example.ams.repository.UserRepository;
import com.example.ams.security.CurrentUserService;

@Service
public class HomeworkService {

	private final HomeworkRepository homeworkRepository;
	private final HomeworkSubmissionRepository submissionRepository;
	private final ClassEnrollmentRepository enrollmentRepository;
	private final UserRepository userRepository;
	private final ClassAccessService classAccessService;
	private final CurrentUserService currentUserService;
	private final ApplicationEventPublisher eventPublisher;

	public HomeworkService(
			HomeworkRepository homeworkRepository,
			HomeworkSubmissionRepository submissionRepository,
			ClassEnrollmentRepository enrollmentRepository,
			UserRepository userRepository,
			ClassAccessService classAccessService,
			CurrentUserService currentUserService,
			ApplicationEventPublisher eventPublisher) {
		this.homeworkRepository = homeworkRepository;
		this.submissionRepository = submissionRepository;
		this.enrollmentRepository = enrollmentRepository;
		this.userRepository = userRepository;
		this.classAccessService = classAccessService;
		this.currentUserService = currentUserService;
		this.eventPublisher = eventPublisher;
	}

	public List<Homework> listHomeworks(long classId) {
		classAccessService.requireReadableClass(classId);
		return homeworkRepository.findByClassId(classId);
	}

	public Homework getHomework(long homeworkId) {
		Homework homework = homeworkRepository.findById(homeworkId)
				.orElseThrow(() -> new BusinessException(ErrorCode.HOMEWORK_NOT_FOUND));
		classAccessService.requireReadableClass(homework.classId());
		return homework;
	}

	public List<SubmissionRow> listSubmissionRows(long homeworkId) {
		Homework homework = getHomework(homeworkId);
		long classId = homework.classId();
		List<Long> studentIds = enrollmentRepository.findByClassId(classId).stream()
				.map(e -> e.studentId())
				.toList();
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
	public Homework createHomework(long classId, String title, Instant dueAt) {
		Clazz clazz = classAccessService.requireReadableClass(classId);
		classAccessService.requireManageClassContent(clazz);
		Homework homework = homeworkRepository.insert(classId, title, dueAt, AssignmentStatus.SCHEDULED);
		for (var e : enrollmentRepository.findByClassId(classId)) {
			submissionRepository.insertEmpty(homework.homeworkId(), e.studentId());
		}
		eventPublisher.publishEvent(new HomeworkCreatedEvent(classId, homework.homeworkId(), title));
		return homework;
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
		HomeworkSubmission updated = submissionRepository.upsert(
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

	public record SubmissionRow(long studentId, String studentName, HomeworkSubmission submission) {
	}
}
