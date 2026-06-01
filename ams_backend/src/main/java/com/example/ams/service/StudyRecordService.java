package com.example.ams.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.ams.api.dto.StudyRecordMetricResponse;
import com.example.ams.api.dto.StudyRecordResponse;
import com.example.ams.api.dto.StudyRecordStudentOptionResponse;
import com.example.ams.api.dto.StudyRecordTestMetricResponse;
import com.example.ams.common.BusinessException;
import com.example.ams.common.ErrorCode;
import com.example.ams.domain.clazz.AssignmentStatus;
import com.example.ams.domain.clazz.ClinicReservation;
import com.example.ams.domain.clazz.Homework;
import com.example.ams.domain.clazz.HomeworkSubmission;
import com.example.ams.domain.clazz.TestExam;
import com.example.ams.domain.clazz.TestScore;
import com.example.ams.domain.study.GaugeLevel;
import com.example.ams.domain.user.User;
import com.example.ams.domain.user.UserRole;
import com.example.ams.repository.ClassEnrollmentRepository;
import com.example.ams.repository.ClinicReservationRepository;
import com.example.ams.repository.HomeworkRepository;
import com.example.ams.repository.HomeworkSubmissionRepository;
import com.example.ams.repository.LessonRecordRepository;
import com.example.ams.repository.TestExamRepository;
import com.example.ams.repository.TestScoreRepository;
import com.example.ams.repository.UserRepository;
import com.example.ams.repository.VideoCertificationRepository;
import com.example.ams.repository.VideoLessonRepository;
import com.example.ams.security.CurrentUserService;

@Service
public class StudyRecordService {

	private final ClassAccessService classAccessService;
	private final CurrentUserService currentUserService;
	private final ClassEnrollmentRepository enrollmentRepository;
	private final UserRepository userRepository;
	private final HomeworkRepository homeworkRepository;
	private final HomeworkSubmissionRepository homeworkSubmissionRepository;
	private final LessonRecordRepository lessonRecordRepository;
	private final ClinicReservationRepository clinicReservationRepository;
	private final TestExamRepository testExamRepository;
	private final TestScoreRepository testScoreRepository;
	private final VideoLessonRepository videoLessonRepository;
	private final VideoCertificationRepository videoCertificationRepository;
	private final AssignmentTargetService assignmentTargetService;

	public StudyRecordService(
			ClassAccessService classAccessService,
			CurrentUserService currentUserService,
			ClassEnrollmentRepository enrollmentRepository,
			UserRepository userRepository,
			HomeworkRepository homeworkRepository,
			HomeworkSubmissionRepository homeworkSubmissionRepository,
			LessonRecordRepository lessonRecordRepository,
			ClinicReservationRepository clinicReservationRepository,
			TestExamRepository testExamRepository,
			TestScoreRepository testScoreRepository,
			VideoLessonRepository videoLessonRepository,
			VideoCertificationRepository videoCertificationRepository,
			AssignmentTargetService assignmentTargetService) {
		this.classAccessService = classAccessService;
		this.currentUserService = currentUserService;
		this.enrollmentRepository = enrollmentRepository;
		this.userRepository = userRepository;
		this.homeworkRepository = homeworkRepository;
		this.homeworkSubmissionRepository = homeworkSubmissionRepository;
		this.lessonRecordRepository = lessonRecordRepository;
		this.clinicReservationRepository = clinicReservationRepository;
		this.testExamRepository = testExamRepository;
		this.testScoreRepository = testScoreRepository;
		this.videoLessonRepository = videoLessonRepository;
		this.videoCertificationRepository = videoCertificationRepository;
		this.assignmentTargetService = assignmentTargetService;
	}

	public StudyRecordResponse getMyRecord(long classId) {
		classAccessService.requireReadableClass(classId);
		UserRole role = currentUserService.requireRole();
		if (role != UserRole.STUDENT) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
		long studentId = currentUserService.requireUserId();
		return buildRecord(classId, studentId);
	}

	public StudyRecordResponse getStudentRecord(long classId, long studentId) {
		classAccessService.requireReadableClass(classId);
		UserRole role = currentUserService.requireRole();
		if (role == UserRole.STUDENT) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
		assertEnrolled(classId, studentId);
		return buildRecord(classId, studentId);
	}

	public List<StudyRecordStudentOptionResponse> listStudentOptions(long classId) {
		classAccessService.requireReadableClass(classId);
		UserRole role = currentUserService.requireRole();
		if (role == UserRole.STUDENT) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
		List<StudyRecordStudentOptionResponse> options = new ArrayList<>();
		for (var enrollment : enrollmentRepository.findByClassId(classId)) {
			User student = userRepository.findById(enrollment.studentId()).orElseThrow();
			options.add(new StudyRecordStudentOptionResponse(student.userId(), student.name()));
		}
		return options;
	}

	private void assertEnrolled(long classId, long studentId) {
		if (!enrollmentRepository.existsByClassIdAndStudentId(classId, studentId)) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "수강생만 조회할 수 있습니다.");
		}
	}

	private StudyRecordResponse buildRecord(long classId, long studentId) {
		User student = userRepository.findById(studentId).orElseThrow();
		LocalDate accessibleFrom = enrollmentRepository.findByClassIdAndStudentId(classId, studentId)
				.map(e -> e.accessibleFrom())
				.orElse(null);
		Map<Long, LocalDate> lessonDateCache = new HashMap<>();

		StudyRecordMetricResponse homework = computeHomework(classId, studentId, accessibleFrom, lessonDateCache);
		StudyRecordMetricResponse clinic = computeClinic(classId, studentId);
		StudyRecordTestMetricResponse test = computeTest(classId, studentId, accessibleFrom, lessonDateCache);
		StudyRecordMetricResponse video = computeVideo(classId, studentId, accessibleFrom, lessonDateCache);

		double overall = homework.ratePercent() * 0.4
				+ clinic.ratePercent() * 0.3
				+ test.ratePercent() * 0.3;
		int overallPercent = (int) Math.round(overall);
		GaugeLevel gaugeLevel = GaugeLevel.fromOverallPercent(overallPercent);

		return new StudyRecordResponse(
				classId,
				studentId,
				student.name(),
				homework,
				clinic,
				test,
				video,
				overallPercent,
				letterGrade(overallPercent),
				gaugeLevel,
				encouragementMessage(gaugeLevel, overallPercent));
	}

	private StudyRecordMetricResponse computeHomework(
			long classId,
			long studentId,
			LocalDate accessibleFrom,
			Map<Long, LocalDate> lessonDateCache) {
		List<Homework> completed = homeworkRepository.findByClassId(classId).stream()
				.filter(h -> h.status() == AssignmentStatus.COMPLETED)
				.filter(h -> isInAccessibleWindow(homeworkRepository.findLessonRecordId(h.homeworkId()), accessibleFrom, lessonDateCache))
				.toList();
		int total = completed.size();
		if (total == 0) {
			return new StudyRecordMetricResponse(0, 0, 0);
		}
		int submitted = 0;
		for (Homework homework : completed) {
			boolean done = homeworkSubmissionRepository
					.findByHomeworkIdAndStudentId(homework.homeworkId(), studentId)
					.map(HomeworkSubmission::submitted)
					.orElse(false);
			if (done) {
				submitted++;
			}
		}
		return new StudyRecordMetricResponse(submitted, total, percent(submitted, total));
	}

	private StudyRecordMetricResponse computeClinic(long classId, long studentId) {
		List<ClinicReservation> reservations = clinicReservationRepository.findByClassIdAndStudentId(classId, studentId);
		int total = reservations.size();
		if (total == 0) {
			return new StudyRecordMetricResponse(0, 0, 0);
		}
		int attended = (int) reservations.stream()
				.filter(r -> Boolean.TRUE.equals(r.resultAttended()))
				.count();
		return new StudyRecordMetricResponse(attended, total, percent(attended, total));
	}

	private StudyRecordTestMetricResponse computeTest(
			long classId,
			long studentId,
			LocalDate accessibleFrom,
			Map<Long, LocalDate> lessonDateCache) {
		List<TestExam> completed = testExamRepository.findByClassId(classId).stream()
				.filter(t -> t.status() == AssignmentStatus.COMPLETED)
				.filter(t -> isInAccessibleWindow(testExamRepository.findLessonRecordId(t.testId()), accessibleFrom, lessonDateCache))
				.toList();
		if (completed.isEmpty()) {
			return new StudyRecordTestMetricResponse(0, 0, 0, null);
		}

		Map<Long, TestExam> latestByRoot = new java.util.HashMap<>();
		for (TestExam test : completed) {
			long rootId = test.rootTestId();
			TestExam existing = latestByRoot.get(rootId);
			if (existing == null || test.retakeAttemptNo() > existing.retakeAttemptNo()) {
				latestByRoot.put(rootId, test);
			}
		}

		int sumScore = 0;
		int scoredCount = 0;
		int attemptedCount = 0;
		for (TestExam test : latestByRoot.values()) {
			Optional<TestScore> scoreOpt = testScoreRepository.findByTestIdAndStudentId(test.testId(), studentId);
			if (scoreOpt.map(TestScore::rawScore).isPresent()) {
				attemptedCount++;
				int pct = StudyRecordGrades.rawScorePercent(scoreOpt.get().rawScore());
				sumScore += pct;
				scoredCount++;
			}
		}
		int avg = scoredCount > 0 ? Math.round((float) sumScore / scoredCount) : 0;
		String latestSummary = completed.stream()
				.max(Comparator.comparing(TestExam::testAt))
				.flatMap(test -> testScoreRepository.findByTestIdAndStudentId(test.testId(), studentId))
				.map(this::formatLatestTestSummary)
				.orElse(null);
		return new StudyRecordTestMetricResponse(avg, attemptedCount, latestByRoot.size(), latestSummary);
	}

	private String formatLatestTestSummary(TestScore score) {
		if (score.rawScore() == null) {
			return null;
		}
		StringBuilder sb = new StringBuilder();
		sb.append(formatScore(score.rawScore())).append("점");
		if (score.classAvg() != null) {
			sb.append(" / 반평균 ").append(formatScore(score.classAvg())).append("점");
		}
		if (score.rank() != null) {
			sb.append(" · ").append(score.rank()).append("등");
		}
		return sb.toString();
	}

	private static String formatScore(BigDecimal value) {
		return value.stripTrailingZeros().toPlainString();
	}

	private StudyRecordMetricResponse computeVideo(
			long classId,
			long studentId,
			LocalDate accessibleFrom,
			Map<Long, LocalDate> lessonDateCache) {
		long total = videoLessonRepository.findByClassId(classId).stream()
				.filter(v -> assignmentTargetService.requiresVideoCertification(
						v.videoId(), classId, studentId))
				.filter(v -> isInAccessibleWindow(videoLessonRepository.findLessonRecordId(v.videoId()), accessibleFrom, lessonDateCache))
				.count();
		if (total == 0) {
			return new StudyRecordMetricResponse(0, 0, 0);
		}
		int certified = (int) videoLessonRepository.findByClassId(classId).stream()
				.filter(v -> assignmentTargetService.requiresVideoCertification(
						v.videoId(), classId, studentId))
				.filter(v -> isInAccessibleWindow(videoLessonRepository.findLessonRecordId(v.videoId()), accessibleFrom, lessonDateCache))
				.filter(v -> videoCertificationRepository
						.findByVideoIdAndStudentId(v.videoId(), studentId)
						.isPresent())
				.count();
		return new StudyRecordMetricResponse(certified, (int) total, percent(certified, (int) total));
	}

	private boolean isInAccessibleWindow(
			Long lessonRecordId,
			LocalDate accessibleFrom,
			Map<Long, LocalDate> lessonDateCache) {
		if (accessibleFrom == null || lessonRecordId == null) {
			return true;
		}
		LocalDate lessonDate = lessonDateCache.computeIfAbsent(
				lessonRecordId,
				id -> lessonRecordRepository.findById(id).map(r -> r.lessonDate()).orElse(null));
		return lessonDate == null || !lessonDate.isBefore(accessibleFrom);
	}

	private static int percent(int numerator, int denominator) {
		return (int) Math.round((numerator * 100.0) / denominator);
	}

	private static String letterGrade(int overallPercent) {
		if (overallPercent >= 90) {
			return "A";
		}
		if (overallPercent >= 75) {
			return "B";
		}
		if (overallPercent >= 60) {
			return "C";
		}
		if (overallPercent >= 50) {
			return "D";
		}
		return "F";
	}

	private static String encouragementMessage(GaugeLevel level, int overallPercent) {
		return switch (level) {
			case GREEN -> overallPercent + "% — 잘 하고 있어요!";
			case ORANGE -> overallPercent + "% — 조금만 더 힘내요.";
			case RED -> overallPercent + "% — 담임 선생님과 상담해 보세요.";
		};
	}
}
