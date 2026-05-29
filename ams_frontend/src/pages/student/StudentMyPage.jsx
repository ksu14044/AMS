import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext'
import { roleLabel } from '../../auth/roleLabels'
import { fetchUnreadNotificationCount } from '../../api/notificationsApi'
import '../../styles/class-list.css'

export default function StudentMyPage() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [unreadCount, setUnreadCount] = useState(0)

  const loadCount = useCallback(async () => {
    try {
      setUnreadCount(await fetchUnreadNotificationCount())
    } catch {
      setUnreadCount(0)
    }
  }, [])

  useEffect(() => {
    loadCount()
  }, [loadCount])

  function handleLogout() {
    logout()
    navigate('/login')
  }

  return (
    <div className="ams-class-list-page">
      <section className="ams-class-list-page__card">
        <h2 className="ams-class-list-page__title">{user?.name ?? '학생'}</h2>
        <p className="ams-class-list-page__desc">{roleLabel(user?.role)}</p>
        {user?.email && <p className="ams-class-list-page__meta">{user.email}</p>}
      </section>

      <div className="ams-my-menu">
        <Link to="/notifications" className="ams-my-menu__item">
          <span>알림</span>
          {unreadCount > 0 && (
            <span className="ams-my-menu__badge">{unreadCount}</span>
          )}
        </Link>
      </div>

      <button
        type="button"
        className="ams-btn ams-btn--ghost ams-btn--block ams-my-logout"
        onClick={handleLogout}
      >
        로그아웃
      </button>
    </div>
  )
}
