package com.example.ams.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.example.ams.event.ClassEnrollmentCreatedEvent;
import com.example.ams.event.ClassNoticeCreatedEvent;
import com.example.ams.event.ClinicResultUpdatedEvent;
import com.example.ams.event.ClinicSlotUpdatedEvent;
import com.example.ams.event.DiligenceReportCreatedEvent;
import com.example.ams.event.HomeworkCreatedEvent;
import com.example.ams.event.HomeworkResultUpdatedEvent;
import com.example.ams.event.TestExamCreatedEvent;
import com.example.ams.event.TestResultUpdatedEvent;
import com.example.ams.event.VideoLessonCreatedEvent;

@Component
public class NotificationEventListener {

	private final NotificationService notificationService;

	public NotificationEventListener(NotificationService notificationService) {
		this.notificationService = notificationService;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onClassNoticeCreated(ClassNoticeCreatedEvent event) {
		notificationService.notifyClassNoticeCreated(event.classId(), event.noticeId(), event.noticeTitle());
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onVideoLessonCreated(VideoLessonCreatedEvent event) {
		notificationService.notifyVideoLessonCreated(event.classId(), event.videoId(), event.title());
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onHomeworkCreated(HomeworkCreatedEvent event) {
		notificationService.notifyHomeworkCreated(event.classId(), event.homeworkId(), event.title());
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onTestExamCreated(TestExamCreatedEvent event) {
		notificationService.notifyTestCreated(event.classId(), event.testId(), event.title());
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onHomeworkResultUpdated(HomeworkResultUpdatedEvent event) {
		notificationService.notifyHomeworkResult(
				event.classId(), event.homeworkId(), event.studentId(), event.title());
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onTestResultUpdated(TestResultUpdatedEvent event) {
		notificationService.notifyTestResult(event.classId(), event.testId(), event.studentId(), event.title());
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onClinicResultUpdated(ClinicResultUpdatedEvent event) {
		notificationService.notifyClinicResult(
				event.classId(), event.reservationId(), event.studentId(), event.slotLabel());
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onClassEnrollmentCreated(ClassEnrollmentCreatedEvent event) {
		notificationService.notifyEnrollmentAssigned(event.classId(), event.studentId());
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onDiligenceReportCreated(DiligenceReportCreatedEvent event) {
		notificationService.notifyDiligenceReportCreated(
				event.classId(), event.studentId(), event.reportId(), event.testTitle());
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onClinicSlotUpdated(ClinicSlotUpdatedEvent event) {
		notificationService.notifyClinicSlotUpdated(
				event.classId(), event.slotId(), event.slotLabel(), event.studentIds());
	}
}
