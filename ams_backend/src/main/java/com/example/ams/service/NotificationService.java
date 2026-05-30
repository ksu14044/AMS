package com.example.ams.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ams.common.BusinessException;
import com.example.ams.common.ClinicBookingPolicy;
import com.example.ams.common.ErrorCode;
import com.example.ams.domain.clazz.Clazz;
import com.example.ams.domain.clazz.ClinicReservation;
import com.example.ams.domain.clazz.ClinicWeek;
import com.example.ams.domain.clazz.Homework;
import com.example.ams.domain.clazz.TestExam;
import com.example.ams.domain.notification.Notification;
import com.example.ams.domain.notification.NotificationReferenceType;
import com.example.ams.domain.notification.NotificationType;
import com.example.ams.repository.ClassEnrollmentRepository;
import com.example.ams.repository.ClinicReservationRepository;
import com.example.ams.repository.ClinicSlotRepository;
import com.example.ams.repository.ClinicWeekRepository;
import com.example.ams.repository.ClazzRepository;
import com.example.ams.repository.HomeworkRepository;
import com.example.ams.repository.NotificationRepository;
import com.example.ams.repository.TestExamRepository;
import com.example.ams.security.CurrentUserService;

@Service
public class NotificationService {

	private final NotificationRepository notificationRepository;
	private final ClassEnrollmentRepository enrollmentRepository;
	private final ClazzRepository clazzRepository;
	private final HomeworkRepository homeworkRepository;
	private final TestExamRepository testExamRepository;
	private final ClinicWeekRepository clinicWeekRepository;
	private final ClinicSlotRepository clinicSlotRepository;
	private final ClinicReservationRepository clinicReservationRepository;
	private final CurrentUserService currentUserService;

	public NotificationService(
			NotificationRepository notificationRepository,
			ClassEnrollmentRepository enrollmentRepository,
			ClazzRepository clazzRepository,
			HomeworkRepository homeworkRepository,
			TestExamRepository testExamRepository,
			ClinicWeekRepository clinicWeekRepository,
			ClinicSlotRepository clinicSlotRepository,
			ClinicReservationRepository clinicReservationRepository,
			CurrentUserService currentUserService) {
		this.notificationRepository = notificationRepository;
		this.enrollmentRepository = enrollmentRepository;
		this.clazzRepository = clazzRepository;
		this.homeworkRepository = homeworkRepository;
		this.testExamRepository = testExamRepository;
		this.clinicWeekRepository = clinicWeekRepository;
		this.clinicSlotRepository = clinicSlotRepository;
		this.clinicReservationRepository = clinicReservationRepository;
		this.currentUserService = currentUserService;
	}

	public List<Notification> listNotifications(boolean unreadOnly) {
		long userId = currentUserService.requireUserId();
		long academyId = currentUserService.requireAcademyId();
		return notificationRepository.findByUserId(userId, academyId, unreadOnly);
	}

	public int getUnreadCount() {
		long userId = currentUserService.requireUserId();
		long academyId = currentUserService.requireAcademyId();
		return notificationRepository.countUnread(userId, academyId);
	}

	@Transactional
	public Notification markRead(long notificationId) {
		long userId = currentUserService.requireUserId();
		Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));
		currentUserService.assertSameAcademy(notification.academyId());
		notificationRepository.markRead(notificationId, userId);
		return notificationRepository.findById(notificationId).orElseThrow();
	}

	@Transactional
	public int markAllRead() {
		long userId = currentUserService.requireUserId();
		long academyId = currentUserService.requireAcademyId();
		return notificationRepository.markAllRead(userId, academyId);
	}

	@Transactional
	public void notifyClassNoticeCreated(long classId, long noticeId, String noticeTitle) {
		Clazz clazz = requireClass(classId);
		String label = NotificationMessages.classLabel(clazz.subject(), clazz.name());
		notifyClassStudents(
				clazz,
				NotificationType.CLASS_NOTICE,
				label + " 새 공지 등록: " + noticeTitle,
				noticeTitle,
				NotificationReferenceType.CLASS_NOTICE,
				noticeId);
	}

	@Transactional
	public void notifyVideoLessonCreated(long classId, long videoId, String title) {
		Clazz clazz = requireClass(classId);
		String label = NotificationMessages.classLabel(clazz.subject(), clazz.name());
		notifyClassStudents(
				clazz,
				NotificationType.VIDEO_LESSON,
				label + " 영상 수업 업로드: " + title,
				title,
				NotificationReferenceType.VIDEO_LESSON,
				videoId);
	}

	@Transactional
	public void notifyHomeworkCreated(long classId, long homeworkId, String title) {
		Clazz clazz = requireClass(classId);
		String label = NotificationMessages.classLabel(clazz.subject(), clazz.name());
		notifyClassStudents(
				clazz,
				NotificationType.HOMEWORK_CREATED,
				label + " 숙제 등록: " + title,
				title,
				NotificationReferenceType.HOMEWORK,
				homeworkId);
	}

	@Transactional
	public void notifyTestCreated(long classId, long testId, String title) {
		Clazz clazz = requireClass(classId);
		String label = NotificationMessages.classLabel(clazz.subject(), clazz.name());
		notifyClassStudents(
				clazz,
				NotificationType.TEST_CREATED,
				label + " 테스트 등록: " + title,
				title,
				NotificationReferenceType.TEST,
				testId);
	}

	@Transactional
	public void notifyHomeworkResult(long classId, long homeworkId, long studentId, String title) {
		Clazz clazz = requireClass(classId);
		String label = NotificationMessages.classLabel(clazz.subject(), clazz.name());
		notifyStudent(
				clazz,
				studentId,
				NotificationType.HOMEWORK_RESULT,
				label + " 숙제 결과 입력: " + title,
				title,
				NotificationReferenceType.HOMEWORK,
				homeworkId);
	}

	@Transactional
	public void notifyTestResult(long classId, long testId, long studentId, String title) {
		Clazz clazz = requireClass(classId);
		String label = NotificationMessages.classLabel(clazz.subject(), clazz.name());
		notifyStudent(
				clazz,
				studentId,
				NotificationType.TEST_RESULT,
				label + " 테스트 결과 입력: " + title,
				title,
				NotificationReferenceType.TEST,
				testId);
	}

	@Transactional
	public void notifyClinicResult(long classId, long reservationId, long studentId, String slotLabel) {
		Clazz clazz = requireClass(classId);
		String label = NotificationMessages.classLabel(clazz.subject(), clazz.name());
		notifyStudent(
				clazz,
				studentId,
				NotificationType.CLINIC_RESULT,
				label + " 클리닉 결과 입력 (" + slotLabel + ")",
				slotLabel,
				NotificationReferenceType.CLINIC_RESERVATION,
				reservationId);
	}

	@Transactional
	public void notifyEnrollmentAssigned(long classId, long studentId) {
		Clazz clazz = requireClass(classId);
		String label = NotificationMessages.classLabel(clazz.subject(), clazz.name());
		notifyStudent(
				clazz,
				studentId,
				NotificationType.ENROLLMENT_ASSIGNED,
				label + " 수강반 배정 완료",
				clazz.name(),
				NotificationReferenceType.ENROLLMENT,
				classId);
	}

	@Transactional
	public void notifyDiligenceReportCreated(long classId, long studentId, long reportId, String testTitle) {
		Clazz clazz = requireClass(classId);
		String label = NotificationMessages.classLabel(clazz.subject(), clazz.name());
		notifyStudent(
				clazz,
				studentId,
				NotificationType.DILIGENCE_REPORT,
				label + " 성실도 보고서 생성: " + testTitle,
				testTitle,
				NotificationReferenceType.DILIGENCE_REPORT,
				reportId);
	}

	@Transactional
	public void notifyClinicSlotUpdated(long classId, long slotId, String slotLabel, List<Long> studentIds) {
		Clazz clazz = requireClass(classId);
		String label = NotificationMessages.classLabel(clazz.subject(), clazz.name());
		for (long studentId : studentIds) {
			notifyStudent(
					clazz,
					studentId,
					NotificationType.CLINIC_SLOT_CHANGED,
					label + " 클리닉 일정 변경 (" + slotLabel + ")",
					slotLabel,
					NotificationReferenceType.CLINIC_SLOT,
					slotId);
		}
	}

	@Transactional
	public void sendHomeworkD1Reminders() {
		// v3.0: due_at 제거 — D-1 숙제 알림 폐기 (Phase 12-6에서 스케줄 제거)
	}

	@Transactional
	public void sendTestD1Reminders() {
		LocalDate tomorrow = LocalDate.now(NotificationMessages.SEOUL).plusDays(1);
		Instant start = NotificationMessages.seoulDayStart(tomorrow).toInstant();
		Instant end = NotificationMessages.seoulDayStart(tomorrow.plusDays(1)).toInstant();
		for (TestExam test : testExamRepository.findScheduledTestBetween(start, end)) {
			Clazz clazz = requireClass(test.classId());
			String label = NotificationMessages.classLabel(clazz.subject(), clazz.name());
			notifyClassStudentsIfAbsent(
					clazz,
					NotificationType.TEST_D1,
					label + " 테스트 내일입니다: " + test.title(),
					test.title(),
					NotificationReferenceType.TEST,
					test.testId());
		}
	}

	@Transactional
	public void lockClinicWeeksAndNotify() {
		LocalDate today = LocalDate.now(ClinicBookingPolicy.SEOUL);
		if (today.getDayOfWeek() != java.time.DayOfWeek.SATURDAY) {
			return;
		}
		LocalDate weekStart = today.with(java.time.DayOfWeek.MONDAY);
		for (ClinicWeek week : clinicWeekRepository.findOpenByWeekStart(weekStart)) {
			clinicWeekRepository.lockWeek(week.classId(), weekStart);
			Clazz clazz = requireClass(week.classId());
			String label = NotificationMessages.classLabel(clazz.subject(), clazz.name());
			var slots = clinicSlotRepository.findByClassIdAndWeekStart(week.classId(), weekStart);
			for (var slot : slots) {
				for (ClinicReservation reservation : clinicReservationRepository.findBySlotId(slot.slotId())) {
					String slotLabel = NotificationMessages.clinicSlotLabel(slot.dayOfWeek(), slot.startTime());
					notifyStudentIfAbsent(
							clazz.academyId(),
							reservation.studentId(),
							NotificationType.CLINIC_CONFIRMED,
							label + " 클리닉 예약 확정 (" + slotLabel + ")",
							slotLabel,
							NotificationReferenceType.CLINIC_RESERVATION,
							reservation.reservationId());
				}
			}
		}
	}

	private void notifyClassStudents(
			Clazz clazz,
			NotificationType type,
			String title,
			String body,
			NotificationReferenceType referenceType,
			long referenceId) {
		for (var enrollment : enrollmentRepository.findByClassId(clazz.classId())) {
			notifyStudent(clazz, enrollment.studentId(), type, title, body, referenceType, referenceId);
		}
	}

	private void notifyClassStudentsIfAbsent(
			Clazz clazz,
			NotificationType type,
			String title,
			String body,
			NotificationReferenceType referenceType,
			long referenceId) {
		for (var enrollment : enrollmentRepository.findByClassId(clazz.classId())) {
			notifyStudentIfAbsent(
					clazz.academyId(),
					enrollment.studentId(),
					type,
					title,
					body,
					referenceType,
					referenceId);
		}
	}

	private void notifyStudent(
			Clazz clazz,
			long studentId,
			NotificationType type,
			String title,
			String body,
			NotificationReferenceType referenceType,
			long referenceId) {
		notificationRepository.insert(
				clazz.academyId(), studentId, type, title, body, referenceType, referenceId);
	}

	private void notifyStudentIfAbsent(
			long academyId,
			long studentId,
			NotificationType type,
			String title,
			String body,
			NotificationReferenceType referenceType,
			long referenceId) {
		if (notificationRepository.existsByUserAndTypeAndReference(
				studentId, type, referenceType, referenceId)) {
			return;
		}
		notificationRepository.insert(academyId, studentId, type, title, body, referenceType, referenceId);
	}

	private Clazz requireClass(long classId) {
		return clazzRepository.findById(classId)
				.orElseThrow(() -> new BusinessException(ErrorCode.CLASS_NOT_FOUND));
	}
}
