package com.example.ams.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.ams.domain.clazz.AssignmentStatus;
import com.example.ams.domain.clazz.ClinicReservation;
import com.example.ams.domain.clazz.ClinicReservationStatus;
import com.example.ams.domain.clazz.ClinicReservationWithSlot;
import com.example.ams.domain.clazz.DayOfWeek;
import com.example.ams.domain.clazz.TestExam;
import com.example.ams.domain.clazz.TestScore;
import com.example.ams.repository.ClinicReservationRepository;
import com.example.ams.repository.HomeworkRepository;
import com.example.ams.repository.HomeworkSubmissionRepository;
import com.example.ams.repository.TestExamRepository;
import com.example.ams.repository.TestScoreRepository;
import com.example.ams.repository.VideoCertificationRepository;
import com.example.ams.repository.VideoLessonRepository;

@ExtendWith(MockitoExtension.class)
class StudyRecordPeriodCalculatorTest {

	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

	@Mock
	private HomeworkRepository homeworkRepository;
	@Mock
	private HomeworkSubmissionRepository homeworkSubmissionRepository;
	@Mock
	private ClinicReservationRepository clinicReservationRepository;
	@Mock
	private VideoLessonRepository videoLessonRepository;
	@Mock
	private VideoCertificationRepository videoCertificationRepository;
	@Mock
	private TestExamRepository testExamRepository;
	@Mock
	private TestScoreRepository testScoreRepository;

	private StudyRecordPeriodCalculator calculator;

	@BeforeEach
	void setUp() {
		calculator = new StudyRecordPeriodCalculator(
				homeworkRepository,
				homeworkSubmissionRepository,
				clinicReservationRepository,
				videoLessonRepository,
				videoCertificationRepository,
				testExamRepository,
				testScoreRepository);
	}

	@Test
	void compute_countsClinicBySlotOccurrenceNotWeekStartDate() {
		long classId = 1L;
		long studentId = 10L;
		Instant periodStart = LocalDate.of(2026, 5, 26).atTime(15, 45).atZone(SEOUL).toInstant();
		Instant periodEnd = LocalDate.of(2026, 5, 28).atTime(13, 0).atZone(SEOUL).toInstant();

		when(homeworkRepository.findByClassId(classId)).thenReturn(List.of());
		when(videoLessonRepository.findByClassId(classId)).thenReturn(List.of());

		ClinicReservation reservation = new ClinicReservation(
				1L, 50L, studentId, "강학생", ClinicReservationStatus.RESERVED, true, null, null, null, Instant.now());
		LocalDate weekStart = LocalDate.of(2026, 5, 25);
		when(clinicReservationRepository.findByClassIdAndStudentIdWithSlot(classId, studentId))
				.thenReturn(List.of(new ClinicReservationWithSlot(
						reservation, weekStart, DayOfWeek.WED, LocalTime.of(14, 0))));

		StudyRecordPeriodMetrics metrics = calculator.compute(classId, studentId, periodStart, periodEnd);

		assertEquals(1, metrics.clinicTotal());
		assertEquals(1, metrics.clinicAttended());
		assertEquals(100, metrics.clinicRate());
	}

	@Test
	void compute_excludesClinicBeforePeriodStartEvenWithEarlierWeekStart() {
		long classId = 1L;
		long studentId = 10L;
		Instant periodStart = LocalDate.of(2026, 5, 26).atTime(15, 45).atZone(SEOUL).toInstant();
		Instant periodEnd = LocalDate.of(2026, 5, 28).atTime(13, 0).atZone(SEOUL).toInstant();

		when(homeworkRepository.findByClassId(classId)).thenReturn(List.of());
		when(videoLessonRepository.findByClassId(classId)).thenReturn(List.of());

		ClinicReservation reservation = new ClinicReservation(
				1L, 50L, studentId, "강학생", ClinicReservationStatus.RESERVED, true, null, null, null, Instant.now());
		when(clinicReservationRepository.findByClassIdAndStudentIdWithSlot(classId, studentId))
				.thenReturn(List.of(new ClinicReservationWithSlot(
						reservation, LocalDate.of(2026, 5, 25), DayOfWeek.TUE, LocalTime.of(10, 0))));

		StudyRecordPeriodMetrics metrics = calculator.compute(classId, studentId, periodStart, periodEnd);

		assertEquals(0, metrics.clinicTotal());
		assertNull(metrics.clinicRate());
	}

	@Test
	void computeTestMetrics_averagesScoredTestsInPeriodAndUsesLatestForRank() {
		long classId = 1L;
		long studentId = 10L;
		Instant periodStart = LocalDate.of(2026, 6, 1).atStartOfDay(SEOUL).toInstant();
		Instant periodEnd = LocalDate.of(2026, 6, 30).atTime(23, 59).atZone(SEOUL).toInstant();
		Instant earlier = LocalDate.of(2026, 6, 5).atTime(10, 0).atZone(SEOUL).toInstant();
		Instant later = LocalDate.of(2026, 6, 15).atTime(10, 0).atZone(SEOUL).toInstant();
		Instant outside = LocalDate.of(2026, 5, 1).atTime(10, 0).atZone(SEOUL).toInstant();

		TestExam inPeriod1 = test(1L, earlier, 0, null);
		TestExam inPeriod2 = test(2L, later, 0, null);
		TestExam outsidePeriod = test(99L, outside, 0, null);
		when(testExamRepository.findByClassId(classId)).thenReturn(List.of(inPeriod1, inPeriod2, outsidePeriod));
		when(testScoreRepository.findByTestIdAndStudentId(eq(1L), eq(studentId)))
				.thenReturn(Optional.of(score(1L, studentId, "80", 3)));
		when(testScoreRepository.findByTestIdAndStudentId(eq(2L), eq(studentId)))
				.thenReturn(Optional.of(score(2L, studentId, "90", 2)));

		StudyRecordPeriodTestMetrics metrics =
				calculator.computeTestMetrics(classId, studentId, periodStart, periodEnd);

		assertEquals(85, metrics.averageScorePercent());
		assertEquals(0, new BigDecimal("85").compareTo(metrics.displayRawScore()));
		assertEquals(2, metrics.latestRank());
		assertEquals(2L, metrics.representativeTestId());
	}

	@Test
	void computeTestMetrics_prefersLatestRetakePerRoot() {
		long classId = 1L;
		long studentId = 10L;
		Instant periodStart = LocalDate.of(2026, 6, 1).atStartOfDay(SEOUL).toInstant();
		Instant periodEnd = LocalDate.of(2026, 6, 30).atTime(23, 59).atZone(SEOUL).toInstant();
		TestExam root = test(10L, LocalDate.of(2026, 6, 5).atTime(10, 0).atZone(SEOUL).toInstant(), 0, null);
		TestExam retake = test(11L, LocalDate.of(2026, 6, 12).atTime(10, 0).atZone(SEOUL).toInstant(), 1, 10L);
		when(testExamRepository.findByClassId(classId)).thenReturn(List.of(root, retake));
		when(testScoreRepository.findByTestIdAndStudentId(eq(11L), eq(studentId)))
				.thenReturn(Optional.of(score(11L, studentId, "80", 2)));

		StudyRecordPeriodTestMetrics metrics =
				calculator.computeTestMetrics(classId, studentId, periodStart, periodEnd);

		assertEquals(80, metrics.averageScorePercent());
		assertEquals(2, metrics.latestRank());
	}

	@Test
	void computeTestMetrics_emptyWhenNoScoredTestsInPeriod() {
		long classId = 1L;
		long studentId = 10L;
		Instant periodStart = LocalDate.of(2026, 6, 1).atStartOfDay(SEOUL).toInstant();
		Instant periodEnd = LocalDate.of(2026, 6, 30).atTime(23, 59).atZone(SEOUL).toInstant();
		when(testExamRepository.findByClassId(classId)).thenReturn(List.of());

		StudyRecordPeriodTestMetrics metrics =
				calculator.computeTestMetrics(classId, studentId, periodStart, periodEnd);

		assertFalse(metrics.hasScoredTest());
		assertNull(metrics.displayRawScore());
	}

	private static TestExam test(long testId, Instant testAt, int retakeAttemptNo, Long parentTestId) {
		return new TestExam(
				testId,
				1L,
				"t",
				testAt,
				AssignmentStatus.COMPLETED,
				null,
				testAt,
				testAt,
				10,
				null,
				null,
				parentTestId,
				retakeAttemptNo);
	}

	private static TestScore score(long testId, long studentId, String raw, int rank) {
		return new TestScore(
				1L,
				testId,
				studentId,
				new BigDecimal(raw),
				null,
				new BigDecimal("70"),
				rank,
				null,
				null,
				null,
				null,
				null,
				Instant.now());
	}
}
