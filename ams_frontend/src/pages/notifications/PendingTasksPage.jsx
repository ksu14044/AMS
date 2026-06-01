import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { subjectLabel } from '../../auth/subjectLabels'
import { fetchPendingTasks } from '../../api/notificationsApi'
import { notifyNotificationsChanged } from '../../api/notificationEvents'
import '../../styles/notifications.css'

const TYPE_LABEL = {
  HOMEWORK: '숙제',
  TEST: '테스트',
  VIDEO: '영상 인증',
  CLINIC: '클리닉',
}

const TAB_BY_TYPE = {
  HOMEWORK: 'homework',
  TEST: 'test',
  VIDEO: 'video',
  CLINIC: 'clinic',
}

export default function PendingTasksPage() {
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      setItems(await fetchPendingTasks())
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    load()
    notifyNotificationsChanged()
  }, [load])

  return (
    <section className="ams-notifications">
      <p className="ams-notifications__summary">
        아직 완료하지 않은 과제 {items.length}건입니다.
      </p>

      {error && <p className="ams-notifications__error">{error}</p>}
      {loading && <p className="ams-notifications__status">불러오는 중…</p>}

      {!loading && items.length === 0 && (
        <p className="ams-notifications__empty">미완료 과제가 없습니다.</p>
      )}

      {!loading && items.length > 0 && (
        <ul className="ams-notifications__list">
          {items.map((item) => (
            <li key={`${item.type}-${item.classId}-${item.entityId}`}>
              <Link
                to={`/classes/${item.classId}?tab=${TAB_BY_TYPE[item.type] ?? 'home'}`}
                className="ams-notifications__item ams-notifications__item--link"
              >
                <span className="ams-notifications__content">
                  <span className="ams-notifications__title">
                    [{TYPE_LABEL[item.type] ?? item.type}] {item.title}
                  </span>
                  <span className="ams-notifications__body">
                    [{subjectLabel(item.subject)}] {item.className}
                  </span>
                </span>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
