package com.example.ams.api.notification;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.ams.api.dto.NotificationResponse;
import com.example.ams.api.dto.UnreadNotificationCountResponse;
import com.example.ams.common.ApiResponse;
import com.example.ams.domain.notification.Notification;
import com.example.ams.service.NotificationService;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

	private final NotificationService notificationService;

	public NotificationController(NotificationService notificationService) {
		this.notificationService = notificationService;
	}

	@GetMapping
	public ApiResponse<List<NotificationResponse>> list(
			@RequestParam(defaultValue = "false") boolean unreadOnly) {
		List<NotificationResponse> items = notificationService.listNotifications(unreadOnly).stream()
				.map(NotificationResponse::from)
				.toList();
		return ApiResponse.ok(items);
	}

	@GetMapping("/unread-count")
	public ApiResponse<UnreadNotificationCountResponse> unreadCount() {
		return ApiResponse.ok(new UnreadNotificationCountResponse(notificationService.getUnreadCount()));
	}

	@PatchMapping("/{notificationId}/read")
	public ApiResponse<NotificationResponse> markRead(@PathVariable long notificationId) {
		Notification updated = notificationService.markRead(notificationId);
		return ApiResponse.ok(NotificationResponse.from(updated));
	}

	@PatchMapping("/read-all")
	public ApiResponse<UnreadNotificationCountResponse> markAllRead() {
		notificationService.markAllRead();
		return ApiResponse.ok(new UnreadNotificationCountResponse(0));
	}
}
