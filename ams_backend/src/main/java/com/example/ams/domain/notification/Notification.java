package com.example.ams.domain.notification;

import java.time.Instant;

public record Notification(
		long notificationId,
		long academyId,
		long userId,
		NotificationType type,
		String title,
		String body,
		NotificationReferenceType referenceType,
		Long referenceId,
		Instant readAt,
		Instant createdAt) {

	public boolean unread() {
		return readAt == null;
	}
}
