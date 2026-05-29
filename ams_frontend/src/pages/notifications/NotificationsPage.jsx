import { useCallback, useEffect, useState } from 'react'
import {
  fetchNotifications,
  markAllNotificationsRead,
  markNotificationRead,
} from '../../api/notificationsApi'
import { notifyNotificationsChanged } from '../../api/notificationEvents'
import '../../styles/notifications.css'

function formatWhen(iso) {
  if (!iso) return ''
  const date = new Date(iso)
  const now = new Date()
  const diffMs = now - date
  const diffMin = Math.floor(diffMs / 60000)
  if (diffMin < 1) return '방금'
  if (diffMin < 60) return `${diffMin}분 전`
  const diffHour = Math.floor(diffMin / 60)
  if (diffHour < 24) return `${diffHour}시간 전`
  const diffDay = Math.floor(diffHour / 24)
  if (diffDay < 7) return `${diffDay}일 전`
  return date.toLocaleDateString('ko-KR')
}

function applyRead(items, notificationId, updated, unreadOnly) {
  if (unreadOnly) {
    return items.filter((item) => item.notificationId !== notificationId)
  }
  return items.map((item) =>
    item.notificationId === notificationId ? { ...item, ...updated, unread: false } : item,
  )
}

export default function NotificationsPage() {
  const [filter, setFilter] = useState('all')
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [markingAll, setMarkingAll] = useState(false)

  const unreadOnly = filter === 'unread'

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      setItems(await fetchNotifications(unreadOnly))
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }, [unreadOnly])

  useEffect(() => {
    load()
  }, [load])

  async function handleMarkRead(notificationId) {
    setError('')
    try {
      const updated = await markNotificationRead(notificationId)
      setItems((prev) => applyRead(prev, notificationId, updated, unreadOnly))
      notifyNotificationsChanged()
    } catch (err) {
      setError(err.message)
      await load()
    }
  }

  async function handleMarkAllRead() {
    setMarkingAll(true)
    setError('')
    try {
      await markAllNotificationsRead()
      if (unreadOnly) {
        setItems([])
      } else {
        setItems((prev) => prev.map((item) => ({ ...item, unread: false })))
      }
      notifyNotificationsChanged()
    } catch (err) {
      setError(err.message)
      await load()
    } finally {
      setMarkingAll(false)
    }
  }

  const hasUnread = items.some((item) => item.unread)

  return (
    <section className="ams-notifications">
      <header className="ams-notifications__toolbar">
        <div className="ams-notifications__filters">
          <button
            type="button"
            className={`ams-notifications__filter${filter === 'all' ? ' ams-notifications__filter--active' : ''}`}
            onClick={() => setFilter('all')}
          >
            전체
          </button>
          <button
            type="button"
            className={`ams-notifications__filter${filter === 'unread' ? ' ams-notifications__filter--active' : ''}`}
            onClick={() => setFilter('unread')}
          >
            미확인
          </button>
        </div>
        <button
          type="button"
          className="ams-btn ams-btn--ghost ams-btn--sm"
          onClick={handleMarkAllRead}
          disabled={markingAll || !hasUnread}
        >
          모두 읽음
        </button>
      </header>

      {error && <p className="ams-notifications__error">{error}</p>}

      {loading && <p className="ams-notifications__status">불러오는 중…</p>}

      {!loading && items.length === 0 && (
        <p className="ams-notifications__empty">알림이 없습니다.</p>
      )}

      {!loading && items.length > 0 && (
        <ul className="ams-notifications__list">
          {items.map((item) => (
            <li key={item.notificationId}>
              <button
                type="button"
                className={`ams-notifications__item${item.unread ? ' ams-notifications__item--unread' : ''}`}
                onClick={() => item.unread && handleMarkRead(item.notificationId)}
              >
                <span className="ams-notifications__dot" aria-hidden={!item.unread} />
                <span className="ams-notifications__content">
                  <span className="ams-notifications__title">{item.title}</span>
                  {item.body !== item.title && (
                    <span className="ams-notifications__body">{item.body}</span>
                  )}
                  <span className="ams-notifications__time">{formatWhen(item.createdAt)}</span>
                </span>
              </button>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
