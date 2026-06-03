package com.example.ams.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.example.ams.domain.clazz.AssignmentStatus;
import com.example.ams.domain.clazz.TestExam;
import com.example.ams.domain.clazz.TestScore;
import com.example.ams.domain.clazz.ClinicReservationWithSlot;
import com.example.ams.domain.clazz.ClinicSlotOccurrence;
import com.example.ams.domain.clazz.Homework;
import com.example.ams.domain.clazz.HomeworkSubmission;
import com.example.ams.domain.clazz.VideoLesson;
import com.example.ams.repository.ClinicReservationRepository;
import com.example.ams.repository.HomeworkRepository;
import com.example.ams.repository.HomeworkSubmissionRepository;
import com.example.ams.repository.TestExamRepository;
import com.example.ams.repository.TestScoreRepository;
import com.example.ams.repository.VideoCertificationRepository;
import com.example.ams.repository.VideoLessonRepository;

@Component
public class StudyRecordPeriodCalculator {

	private final HomeworkRepository homeworkRepository;
	private final HomeworkSubmissionRepository homeworkSubmissionRepository;
	private final ClinicReservationRepository clinicReservationRepository;
	private final VideoLessonRepository videoLessonRepository;
	private final VideoCertificationRepository videoCertificationRepository;
	private final TestExamRepository testExamRepository;
	private final TestScoreRepository testScoreRepository;

	public StudyRecordPeriodCalculator(
			HomeworkRepository homeworkRepository,
			HomeworkSubmissionRepository homeworkSubmissionRepository,
			ClinicReservationRepository clinicReservationRepository,
			VideoLessonRepository videoLessonRepository,
			VideoCertificationRepository videoCertificationRepository,
			TestExamRepository testExamRepository,
			TestScoreRepository testScoreRepository) {
		this.homeworkRepository = homeworkRepository;
		this.homeworkSubmissionRepository = homeworkSubmissionRepository;
		this.clinicReservationRepository = clinicReservationRepository;
		this.videoLessonRepository = videoLessonRepository;
		this.videoCertificationRepository = videoCertificationRepository;
		this.testExamRepository = testExamRepository;
		this.testScoreRepository = testScoreRepository;
	}

	public StudyRecordPeriodMetrics compute(
			long classId,
			long studentId,
			Instant periodStart,
			Instant periodEnd) {
		int homeworkSubmitted = 0;
		int homeworkTotal = 0;
		for (Homework homework : homeworkRepository.findByClassId(classId)) {
			if (homework.status() != AssignmentStatus.COMPLETED) {
				continue;
			}
			if (!isInPeriod(homework.createdAt(), periodStart, periodEnd)) {
				continue;
			}
			homeworkTotal++;
			boolean submitted = homeworkSubmissionRepository
					.findByHomeworkIdAndStudentId(homework.homeworkId(), studentId)
					.map(HomeworkSubmission::submitted)
					.orElse(false);
			if (submitted) {
				homeworkSubmitted++;
			}
		}

		List<ClinicReservationWithSlot> clinicInPeriod = clinicReservationRepository
				.findByClassIdAndStudentIdWithSlot(classId, studentId)
				.stream()
				.filter(r -> ClinicSlotOccurrence.isInPeriod(
						r.weekStartDate(),
						r.dayOfWeek(),
						r.startTime(),
						periodStart,
						periodEnd))
				.toList();
		int clinicTotal = clinicInPeriod.size();
		int clinicAttended = (int) clinicInPeriod.stream()
				.filter(r -> Boolean.TRUE.equals(r.reservation().resultAttended()))
				.count();

		int videoTotal = 0;
		int videoCertified = 0;
		for (VideoLesson video : videoLessonRepository.findByClassId(classId)) {
			if (!isInPeriod(video.publishedAt(), periodStart, periodEnd)) {
				continue;
			}
			videoTotal++;
			if (videoCertificationRepository.findByVideoIdAndStudentId(video.videoId(), studentId).isPresent()) {
				videoCertified++;
			}
		}

		return new StudyRecordPeriodMetrics(
				homeworkSubmitted,
				homeworkTotal,
				StudyRecordGrades.rateOrNull(homeworkSubmitted, homeworkTotal),
				clinicAttended,
				clinicTotal,
				StudyRecordGrades.rateOrNull(clinicAttended, clinicTotal),
				videoCertified,
				videoTotal,
				StudyRecordGrades.rateOrNull(videoCertified, videoTotal));
	}

	/**
	 * 기간 내 완료된 시험을 루트별 최신 재시험으로 묶어 점수 % 평균을 낸다.
	 * 석차·반평균은 기간 내 가장 최근 시험 점수 행을 따른다 (공부기록과 동일 원칙).
	 */
	public StudyRecordPeriodTestMetrics computeTestMetrics(
			long classId,
			long studentId,
			Instant periodStart,
			Instant periodEnd) {
		Map<Long, TestExam> latestByRoot = new HashMap<>();
		for (TestExam test : testExamRepository.findByClassId(classId)) {
			if (test.status() != AssignmentStatus.COMPLETED || !isInPeriod(test.testAt(), periodStart, periodEnd)) {
				continue;
			}
			long rootId = test.rootTestId();
			TestExam existing = latestByRoot.get(rootId);
			if (existing == null || test.retakeAttemptNo() > existing.retakeAttemptNo()) {
				latestByRoot.put(rootId, test);
			}
		}
		if (latestByRoot.isEmpty()) {
			return StudyRecordPeriodTestMetrics.empty();
		}

		int sumScore = 0;
		int scoredCount = 0;
		for (TestExam test : latestByRoot.values()) {
			Optional<TestScore> scoreOpt = testScoreRepository.findByTestIdAndStudentId(test.testId(), studentId);
			if (scoreOpt.map(TestScore::rawScore).isPresent()) {
				sumScore += StudyRecordGrades.rawScorePercent(scoreOpt.get().rawScore());
				scoredCount++;
			}
		}
		if (scoredCount == 0) {
			return StudyRecordPeriodTestMetrics.empty();
		}
		int averagePercent = Math.round((float) sumScore / scoredCount);
		BigDecimal displayRawScore = BigDecimal.valueOf(averagePercent);

		TestExam latestTest = latestByRoot.values().stream()
				.max(Comparator.comparing(TestExam::testAt).thenComparing(TestExam::testId))
				.orElseThrow();
		Optional<TestScore> latestScore = testScoreRepository.findByTestIdAndStudentId(latestTest.testId(), studentId);
		return new StudyRecordPeriodTestMetrics(
				averagePercent,
				true,
				displayRawScore,
				latestScore.map(TestScore::classAvg).orElse(null),
				latestScore.map(TestScore::rank).orElse(null),
				latestTest.testId());
	}

	public StudyRecordPeriodTestMetrics computeTestMetricsForSingleExam(TestExam test, long studentId) {
		Optional<TestScore> scoreOpt = testScoreRepository.findByTestIdAndStudentId(test.testId(), studentId);
		BigDecimal raw = scoreOpt.map(TestScore::rawScore).orElse(null);
		if (raw == null) {
			return StudyRecordPeriodTestMetrics.empty();
		}
		int percent = StudyRecordGrades.rawScorePercent(raw);
		return new StudyRecordPeriodTestMetrics(
				percent,
				true,
				raw,
				scoreOpt.map(TestScore::classAvg).orElse(null),
				scoreOpt.map(TestScore::rank).orElse(null),
				test.testId());
	}

	static boolean isInPeriod(Instant instant, Instant start, Instant end) {
		return !instant.isBefore(start) && !instant.isAfter(end);
	}
}
