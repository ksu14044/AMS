package com.example.ams.api.dto;

import java.time.Instant;

import com.example.ams.domain.notification.Notification;
import com.example.ams.domain.notification.NotificationReferenceType;
import com.example.ams.domain.notification.NotificationType;

public record NotificationResponse(
		long notificationId,
		NotificationType type,
		String title,
		String body,
		NotificationReferenceType referenceType,
		Long referenceId,
		boolean unread,
		Instant readAt,
		Instant createdAt) {

	public static NotificationResponse from(Notification notification) {
		return new NotificationResponse(
				notification.notificationId(),
				notification.type(),
				notification.title(),
				notification.body(),
				notification.referenceType(),
				notification.referenceId(),
				notification.unread(),
				notification.readAt(),
				notification.createdAt());
	}
}
