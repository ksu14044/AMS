package com.example.ams.domain.notification;

import java.time.Instant;

import com.example.ams.domain.notification.NotificationReferenceType;
import com.example.ams.domain.notification.NotificationStatus;
import com.example.ams.domain.notification.NotificationType;

public record Notification(
		long notificationId,
		long academyId,
		long userId,
		NotificationType type,
		String title,
		String body,
		NotificationReferenceType referenceType,
		Long referenceId,
		NotificationStatus status,
		Instant readAt,
		Instant createdAt) {

	public boolean unread() {
		return readAt == null;
	}
}
