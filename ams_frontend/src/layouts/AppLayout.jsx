import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useMemo } from 'react'
import { useAuth } from '../auth/AuthContext'
import { roleLabel } from '../auth/roleLabels'
import NotificationBell from '../components/NotificationBell'
import BottomNav from '../components/BottomNav'
import '../styles/app-layout.css'
import '../styles/bottom-nav.css'

const STUDENT_TAB_PATHS = ['/student', '/student/clinic', '/student/records', '/student/my']
const STUDENT_TAB_TITLES = {
  '/student/clinic': '클리닉',
  '/student/records': '공부 기록',
  '/student/my': 'MY',
}

const ROSTER_PATHS = ['/admin/students', '/teacher/students']

export default function AppLayout({ title }) {
  const { user, logout, homePath } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const isStudent = user?.role === 'STUDENT'
  const isParent = user?.role === 'PARENT'
  const isRosterPage = ROSTER_PATHS.includes(location.pathname)
  const isClassDetailPage = location.pathname.startsWith('/classes/')
  const isStudentTab = isStudent && STUDENT_TAB_PATHS.includes(location.pathname)
  const showBottomNav =
    isStudent &&
    !isClassDetailPage &&
    location.pathname !== '/notifications'
  const useWideLayout =
    isClassDetailPage || isRosterPage || isParent || (!isStudent && !isStudentTab)

  const pageTitle = useMemo(() => {
    if (STUDENT_TAB_TITLES[location.pathname]) return STUDENT_TAB_TITLES[location.pathname]
    if (user?.role === 'STAFF_OFFICE' && title === '선생님 홈') return '행정 홈'
    return title
  }, [location.pathname, title, user?.role])

  function handleLogout() {
    logout()
    navigate('/login')
  }

  const studentGreeting =
    location.pathname === '/student'
      ? `안녕하세요${user?.name ? `, ${user.name}님` : ''} 👋`
      : pageTitle

  return (
    <div
      className={`ams-app${showBottomNav ? ' ams-app--with-bottom-nav' : ''}${
        isStudentTab ? ' ams-app--student' : ''
      }${isRosterPage ? ' ams-app--roster-page' : ''}${
        isClassDetailPage ? ' ams-app--class-detail' : ''
      }${useWideLayout ? ' ams-app--wide' : ''}`}
    >
      {isStudentTab ? (
        <header className="ams-app__header ams-app__header--student">
          <h1 className="ams-app__student-title">{studentGreeting}</h1>
          <div className="ams-app__actions">
            <NotificationBell />
            <button
              type="button"
              className="ams-btn ams-btn--ghost ams-btn--sm"
              onClick={handleLogout}
            >
              로그아웃
            </button>
          </div>
        </header>
      ) : (
        <header className="ams-app__header">
          <div>
            <p className="ams-app__eyebrow">{roleLabel(user.role)}</p>
            <h1 className="ams-app__title">{pageTitle}</h1>
          </div>
          <div className="ams-app__actions">
            <NotificationBell />
            <Link to={homePath} className="ams-app__link">
              홈
            </Link>
            <button
              type="button"
              className="ams-btn ams-btn--ghost ams-btn--sm"
              onClick={handleLogout}
            >
              로그아웃
            </button>
          </div>
        </header>
      )}
      <main className="ams-app__main">
        <div className="ams-app__container">
          <Outlet />
        </div>
      </main>
      {showBottomNav && <BottomNav />}
    </div>
  )
}
