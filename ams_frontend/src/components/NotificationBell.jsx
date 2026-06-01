import { useCallback, useEffect, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { fetchNotificationBadgeCount } from '../api/notificationsApi'
import { NOTIFICATIONS_CHANGED } from '../api/notificationEvents'
import '../styles/notifications.css'

export default function NotificationBell() {
  const [count, setCount] = useState(0)
  const location = useLocation()

  const load = useCallback(async () => {
    try {
      const badge = await fetchNotificationBadgeCount()
      setCount(badge.count)
    } catch {
      setCount(0)
    }
  }, [])

  useEffect(() => {
    load()
  }, [load, location.pathname])

  useEffect(() => {
    function handleChange() {
      load()
    }
    window.addEventListener(NOTIFICATIONS_CHANGED, handleChange)
    window.addEventListener('focus', handleChange)
    return () => {
      window.removeEventListener(NOTIFICATIONS_CHANGED, handleChange)
      window.removeEventListener('focus', handleChange)
    }
  }, [load])

  return (
    <Link
      to="/notifications"
      className="ams-icon-btn ams-notification-bell"
      aria-label={count > 0 ? `알림 ${count}건` : '알림'}
    >
      <svg
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth={1.8}
        strokeLinecap="round"
        strokeLinejoin="round"
        aria-hidden
      >
        <path d="M6 16V11a6 6 0 1 1 12 0v5l1.5 2h-15L6 16Z" />
        <path d="M10 20a2 2 0 0 0 4 0" />
      </svg>
      {count > 0 && (
        <span className="ams-notification-bell__badge" aria-hidden>
          {count > 99 ? '99+' : count}
        </span>
      )}
    </Link>
  )
}
