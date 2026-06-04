import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext'
import { homePathForRole } from '../../auth/roleLabels'

export default function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ email: '', password: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const result = await login(form)
      if (result.needsAcademySelection) {
        navigate('/login/select', {
          state: {
            loginToken: result.loginToken,
            academies: result.academies,
          },
        })
        return
      }
      const user = result.user
      if (user.status === 'PENDING') {
        navigate('/pending')
      } else {
        navigate(homePathForRole(user.role))
      }
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="ams-card ams-auth__card">
      <h1 className="ams-card__title">로그인</h1>
      <p className="ams-card__desc">이메일과 비밀번호로 로그인합니다.</p>
      <form className="ams-form" onSubmit={handleSubmit}>
        <label className="ams-field">
          <span>이메일</span>
          <input
            type="email"
            value={form.email}
            onChange={(e) => setForm({ ...form, email: e.target.value })}
            required
          />
        </label>
        <label className="ams-field">
          <span>비밀번호</span>
          <input
            type="password"
            value={form.password}
            onChange={(e) => setForm({ ...form, password: e.target.value })}
            minLength={8}
            required
          />
        </label>
        {error && <p className="ams-form__error">{error}</p>}
        <button
          type="submit"
          className="ams-btn ams-btn--primary ams-btn--block ams-form__submit"
          disabled={loading}
        >
          {loading ? '로그인 중…' : '로그인'}
        </button>
      </form>
      <p className="ams-card__desc ams-auth__invite-hint">
        학생·교직원·학부모 가입은 학원에서 받은 전용 링크로만 가능합니다.
      </p>
      <div className="ams-card__footer">
        <Link to="/signup">새 학원 개설</Link>
      </div>
    </div>
  )
}
