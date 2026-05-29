package com.example.ams.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.ams.domain.clazz.ClinicReservation;
import com.example.ams.domain.clazz.ClinicReservationStatus;
import com.example.ams.domain.clazz.ClinicReservationWithSlot;
import com.example.ams.domain.clazz.DayOfWeek;
import com.example.ams.repository.ClinicReservationRepository;
import com.example.ams.repository.HomeworkRepository;
import com.example.ams.repository.HomeworkSubmissionRepository;
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

	private StudyRecordPeriodCalculator calculator;

	@BeforeEach
	void setUp() {
		calculator = new StudyRecordPeriodCalculator(
				homeworkRepository,
				homeworkSubmissionRepository,
				clinicReservationRepository,
				videoLessonRepository,
				videoCertificationRepository);
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
				1L, 50L, studentId, "강학생", ClinicReservationStatus.RESERVED, true, null, Instant.now());
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
				1L, 50L, studentId, "강학생", ClinicReservationStatus.RESERVED, true, null, Instant.now());
		when(clinicReservationRepository.findByClassIdAndStudentIdWithSlot(classId, studentId))
				.thenReturn(List.of(new ClinicReservationWithSlot(
						reservation, LocalDate.of(2026, 5, 25), DayOfWeek.TUE, LocalTime.of(10, 0))));

		StudyRecordPeriodMetrics metrics = calculator.compute(classId, studentId, periodStart, periodEnd);

		assertEquals(0, metrics.clinicTotal());
		assertNull(metrics.clinicRate());
	}
}
