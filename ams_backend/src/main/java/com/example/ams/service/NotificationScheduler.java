package com.example.ams.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NotificationScheduler {

	private final NotificationService notificationService;

	public NotificationScheduler(NotificationService notificationService) {
		this.notificationService = notificationService;
	}

	@Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
	public void sendTestDayBeforeReminders() {
		notificationService.sendTestD1Reminders();
	}

	@Scheduled(cron = "0 0 23 * * SAT", zone = "Asia/Seoul")
	public void lockClinicWeeks() {
		notificationService.lockClinicWeeksAndNotify();
	}
}
