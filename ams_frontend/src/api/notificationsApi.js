import { apiRequest } from './client'

const NO_CACHE = { cache: 'no-store' }

export function fetchNotifications(unreadOnly = false) {
  const query = unreadOnly ? '?unreadOnly=true' : ''
  return apiRequest(`/notifications${query}`, NO_CACHE)
}

export function fetchUnreadNotificationCount() {
  return apiRequest('/notifications/unread-count', NO_CACHE).then((data) => data.count)
}

export function markNotificationRead(notificationId) {
  return apiRequest(`/notifications/${notificationId}/read`, { method: 'PATCH' })
}

export function markAllNotificationsRead() {
  return apiRequest('/notifications/read-all', { method: 'PATCH' })
}
