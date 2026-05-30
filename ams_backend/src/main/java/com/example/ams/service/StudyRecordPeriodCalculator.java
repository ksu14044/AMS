package com.example.ams.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Component;

import com.example.ams.domain.clazz.AssignmentStatus;
import com.example.ams.domain.clazz.ClinicReservationWithSlot;
import com.example.ams.domain.clazz.ClinicSlotOccurrence;
import com.example.ams.domain.clazz.Homework;
import com.example.ams.domain.clazz.HomeworkSubmission;
import com.example.ams.domain.clazz.VideoLesson;
import com.example.ams.repository.ClinicReservationRepository;
import com.example.ams.repository.HomeworkRepository;
import com.example.ams.repository.HomeworkSubmissionRepository;
import com.example.ams.repository.VideoCertificationRepository;
import com.example.ams.repository.VideoLessonRepository;

@Component
public class StudyRecordPeriodCalculator {

	private final HomeworkRepository homeworkRepository;
	private final HomeworkSubmissionRepository homeworkSubmissionRepository;
	private final ClinicReservationRepository clinicReservationRepository;
	private final VideoLessonRepository videoLessonRepository;
	private final VideoCertificationRepository videoCertificationRepository;

	public StudyRecordPeriodCalculator(
			HomeworkRepository homeworkRepository,
			HomeworkSubmissionRepository homeworkSubmissionRepository,
			ClinicReservationRepository clinicReservationRepository,
			VideoLessonRepository videoLessonRepository,
			VideoCertificationRepository videoCertificationRepository) {
		this.homeworkRepository = homeworkRepository;
		this.homeworkSubmissionRepository = homeworkSubmissionRepository;
		this.clinicReservationRepository = clinicReservationRepository;
		this.videoLessonRepository = videoLessonRepository;
		this.videoCertificationRepository = videoCertificationRepository;
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

	static boolean isInPeriod(Instant instant, Instant start, Instant end) {
		return !instant.isBefore(start) && !instant.isAfter(end);
	}
}
