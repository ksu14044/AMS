export const NOTIFICATIONS_CHANGED = 'ams-notifications-changed'

export function notifyNotificationsChanged() {
  window.dispatchEvent(new Event(NOTIFICATIONS_CHANGED))
}
