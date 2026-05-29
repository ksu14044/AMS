import { useState } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext'
import { homePathForRole, roleLabel } from '../../auth/roleLabels'

export default function LoginAcademySelectPage() {
  const { completeLogin } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const { loginToken, academies } = location.state ?? {}
  const [error, setError] = useState('')
  const [loadingUserId, setLoadingUserId] = useState(null)

  if (!loginToken || !academies?.length) {
    return <Navigate to="/login" replace />
  }

  async function handleSelect(userId) {
    setError('')
    setLoadingUserId(userId)
    try {
      const user = await completeLogin({ loginToken, userId })
      if (user.status === 'PENDING') {
        navigate('/pending')
      } else {
        navigate(homePathForRole(user.role))
      }
    } catch (err) {
      setError(err.message)
    } finally {
      setLoadingUserId(null)
    }
  }

  return (
    <div className="ams-card ams-auth__card">
      <h1 className="ams-card__title">소속 선택</h1>
      <p className="ams-card__desc">로그인할 소속을 선택해 주세요.</p>
      <ul className="ams-list ams-auth__academy-list">
        {academies.map((option) => (
          <li key={option.userId}>
            <button
              type="button"
              className="ams-btn ams-btn--block ams-auth__academy-option"
              disabled={loadingUserId !== null}
              onClick={() => handleSelect(option.userId)}
            >
              <span className="ams-auth__academy-option-name">{option.academyName}</span>
              <span className="ams-auth__academy-option-meta">
                {option.name} · {roleLabel(option.role)}
              </span>
              {loadingUserId === option.userId ? '로그인 중…' : null}
            </button>
          </li>
        ))}
      </ul>
      {error && <p className="ams-form__error">{error}</p>}
      <div className="ams-card__footer">
        <Link to="/login">로그인으로 돌아가기</Link>
      </div>
    </div>
  )
}
