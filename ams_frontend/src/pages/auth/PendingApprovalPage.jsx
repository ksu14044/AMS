import { Navigate, useNavigate } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext'
import { roleLabel } from '../../auth/roleLabels'

export default function PendingApprovalPage() {
  const { user, logout, isAuthenticated } = useAuth()
  const navigate = useNavigate()

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  function handleLogout() {
    logout()
    navigate('/login')
  }

  return (
    <div className="ams-card ams-auth__card ams-card--center">
      <div className="ams-pending">
        <span className="ams-pending__badge" aria-hidden>
          ⏳
        </span>
        <h1 className="ams-pending__title">승인 대기</h1>
        {user?.role && <span className="ams-pending__role">{roleLabel(user.role)}</span>}
        <p className="ams-pending__desc">
          {user?.name ? `${user.name}님의 ` : ''}계정은 원장 승인 후 이용할 수 있습니다.
          <br />
          승인 알림을 받은 뒤 다시 로그인해 주세요.
        </p>
        <button
          type="button"
          className="ams-btn ams-btn--ghost ams-btn--block"
          onClick={handleLogout}
        >
          로그아웃
        </button>
      </div>
    </div>
  )
}
