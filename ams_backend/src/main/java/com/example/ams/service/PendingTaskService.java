package com.example.ams.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.ams.domain.clazz.AssignmentEntityType;
import com.example.ams.domain.clazz.Clazz;
import com.example.ams.domain.clazz.ClinicReservation;
import com.example.ams.domain.clazz.Homework;
import com.example.ams.domain.clazz.HomeworkSubmission;
import com.example.ams.domain.clazz.TestExam;
import com.example.ams.domain.clazz.TestScore;
import com.example.ams.domain.clazz.VideoLesson;
import com.example.ams.domain.pending.PendingTaskType;
import com.example.ams.domain.user.Subject;
import com.example.ams.repository.ClassEnrollmentRepository;
import com.example.ams.repository.ClinicReservationRepository;
import com.example.ams.repository.ClazzRepository;
import com.example.ams.repository.HomeworkRepository;
import com.example.ams.repository.HomeworkSubmissionRepository;
import com.example.ams.repository.LessonRecordRepository;
import com.example.ams.repository.TestExamRepository;
import com.example.ams.repository.TestScoreRepository;
import com.example.ams.repository.VideoCertificationRepository;
import com.example.ams.repository.VideoLessonRepository;

@Service
public class PendingTaskService {

	private final ClazzRepository clazzRepository;
	private final ClassEnrollmentRepository enrollmentRepository;
	private final HomeworkRepository homeworkRepository;
	private final HomeworkSubmissionRepository homeworkSubmissionRepository;
	private final TestExamRepository testExamRepository;
	private final TestScoreRepository testScoreRepository;
	private final VideoLessonRepository videoLessonRepository;
	private final VideoCertificationRepository videoCertificationRepository;
	private final ClinicReservationRepository clinicReservationRepository;
	private final LessonRecordRepository lessonRecordRepository;
	private final AssignmentTargetService assignmentTargetService;

	public PendingTaskService(
			ClazzRepository clazzRepository,
			ClassEnrollmentRepository enrollmentRepository,
			HomeworkRepository homeworkRepository,
			HomeworkSubmissionRepository homeworkSubmissionRepository,
			TestExamRepository testExamRepository,
			TestScoreRepository testScoreRepository,
			VideoLessonRepository videoLessonRepository,
			VideoCertificationRepository videoCertificationRepository,
			ClinicReservationRepository clinicReservationRepository,
			LessonRecordRepository lessonRecordRepository,
			AssignmentTargetService assignmentTargetService) {
		this.clazzRepository = clazzRepository;
		this.enrollmentRepository = enrollmentRepository;
		this.homeworkRepository = homeworkRepository;
		this.homeworkSubmissionRepository = homeworkSubmissionRepository;
		this.testExamRepository = testExamRepository;
		this.testScoreRepository = testScoreRepository;
		this.videoLessonRepository = videoLessonRepository;
		this.videoCertificationRepository = videoCertificationRepository;
		this.clinicReservationRepository = clinicReservationRepository;
		this.lessonRecordRepository = lessonRecordRepository;
		this.assignmentTargetService = assignmentTargetService;
	}

	public int countPending(long studentId) {
		return listPending(studentId).size();
	}

	public List<PendingTaskItem> listPending(long studentId) {
		List<PendingTaskItem> items = new ArrayList<>();
		for (Clazz clazz : clazzRepository.findByStudentId(studentId)) {
			LocalDate accessibleFrom = enrollmentRepository.findByClassIdAndStudentId(clazz.classId(), studentId)
					.map(e -> e.accessibleFrom())
					.orElse(null);
			collectForClass(items, clazz, studentId, accessibleFrom);
		}
		return items;
	}

	private void collectForClass(
			List<PendingTaskItem> items,
			Clazz clazz,
			long studentId,
			LocalDate accessibleFrom) {
		long classId = clazz.classId();
		Map<Long, LocalDate> lessonDateCache = new HashMap<>();

		for (Homework homework : homeworkRepository.findByClassId(classId)) {
			if (!assignmentTargetService.canStudentAccess(
					AssignmentEntityType.HOMEWORK, homework.homeworkId(), classId, studentId)) {
				continue;
			}
			if (!isLessonVisible(homeworkRepository.findLessonRecordId(homework.homeworkId()), accessibleFrom, lessonDateCache)) {
				continue;
			}
			HomeworkSubmission submission = homeworkSubmissionRepository
					.findByHomeworkIdAndStudentId(homework.homeworkId(), studentId)
					.orElse(null);
			if (submission == null || submission.score() == null) {
				items.add(task(PendingTaskType.HOMEWORK, clazz, homework.homeworkId(), homework.title()));
			}
		}

		for (TestExam test : testExamRepository.findByClassId(classId)) {
			if (test.isRetake()) {
				if (testScoreRepository.findByTestIdAndStudentId(test.testId(), studentId).isEmpty()) {
					continue;
				}
			} else if (!assignmentTargetService.canStudentAccess(
					AssignmentEntityType.TEST, test.testId(), classId, studentId)) {
				continue;
			}
			if (!isLessonVisible(testExamRepository.findLessonRecordId(test.testId()), accessibleFrom, lessonDateCache)) {
				continue;
			}
			TestScore score = testScoreRepository.findByTestIdAndStudentId(test.testId(), studentId).orElse(null);
			if (score == null || score.rawScore() == null) {
				items.add(task(PendingTaskType.TEST, clazz, test.testId(), test.title()));
			}
		}

		for (VideoLesson video : videoLessonRepository.findByClassId(classId)) {
			if (!assignmentTargetService.requiresVideoCertification(video.videoId(), classId, studentId)) {
				continue;
			}
			if (!isLessonVisible(videoLessonRepository.findLessonRecordId(video.videoId()), accessibleFrom, lessonDateCache)) {
				continue;
			}
			if (videoCertificationRepository.findByVideoIdAndStudentId(video.videoId(), studentId).isEmpty()) {
				items.add(task(PendingTaskType.VIDEO, clazz, video.videoId(), video.title()));
			}
		}

		for (ClinicReservation reservation : clinicReservationRepository.findByClassIdAndStudentId(classId, studentId)) {
			if (reservation.resultAttended() == null) {
				items.add(task(PendingTaskType.CLINIC, clazz, reservation.reservationId(), "클리닉 결과 대기"));
			}
		}
	}

	private static PendingTaskItem task(PendingTaskType type, Clazz clazz, long entityId, String title) {
		return new PendingTaskItem(type, clazz.classId(), clazz.name(), clazz.subject(), entityId, title);
	}

	private boolean isLessonVisible(
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

	public record PendingTaskItem(
			PendingTaskType type,
			long classId,
			String className,
			Subject subject,
			long entityId,
			String title) {
	}

}
