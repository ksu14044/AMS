import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import PhoneNumberField from '../../components/PhoneNumberField'
import { useAuth } from '../../auth/AuthContext'
import SignupInviteGate from '../../components/auth/SignupInviteGate'
import { useSignupInvite } from '../../hooks/useSignupInvite'

export default function SignupParentPage() {
  const { signupParent } = useAuth()
  const navigate = useNavigate()
  const { token, invite, loading, error } = useSignupInvite('PARENT')
  const [form, setForm] = useState({
    name: '',
    email: '',
    password: '',
    confirmPassword: '',
    phoneNumber: '',
    personalInfoConsent: false,
  })
  const [submitError, setSubmitError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(e) {
    e.preventDefault()
    setSubmitError('')
    if (form.password !== form.confirmPassword) {
      setSubmitError('비밀번호와 비밀번호 확인이 일치하지 않습니다.')
      return
    }
    setSubmitting(true)
    try {
      await signupParent({
        inviteToken: token,
        academyCode: invite.academyCode,
        name: form.name,
        email: form.email,
        password: form.password,
        phoneNumber: form.phoneNumber,
        personalInfoConsent: form.personalInfoConsent,
      })
      navigate('/parent')
    } catch (err) {
      setSubmitError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <SignupInviteGate loading={loading} error={error} invite={invite}>
      {(resolvedInvite) => (
        <div className="ams-card ams-auth__card">
          <h1 className="ams-card__title">학부모 가입</h1>
          <p className="ams-card__desc">
            {resolvedInvite.academyName && (
              <>
                <strong>{resolvedInvite.academyName}</strong>
                <br />
              </>
            )}
            가입 후 학원에서 자녀와 연결해 주시면 공부기록·보고서를 조회할 수 있습니다.
          </p>
          <form className="ams-form" onSubmit={handleSubmit}>
            <label className="ams-field ams-field--locked">
              <span>학원 코드</span>
              <input
                className="ams-input--locked"
                value={resolvedInvite.academyCode}
                readOnly
                disabled
                aria-readonly="true"
              />
            </label>
            <label className="ams-field">
              <span>이름</span>
              <input
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
                required
              />
            </label>
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
            <label className="ams-field">
              <span>비밀번호 확인</span>
              <input
                type="password"
                value={form.confirmPassword}
                onChange={(e) => setForm({ ...form, confirmPassword: e.target.value })}
                minLength={8}
                required
              />
            </label>
            <PhoneNumberField
              value={form.phoneNumber}
              onChange={(phoneNumber) => setForm({ ...form, phoneNumber })}
            />
            <label className="ams-consent">
              <input
                type="checkbox"
                checked={form.personalInfoConsent}
                onChange={(e) => setForm({ ...form, personalInfoConsent: e.target.checked })}
                required
              />
              <span>
                <strong>[필수] 개인정보 수집 및 이용 동의</strong>
                <br />
                본인은 AMS 서비스 회원가입과 학원 운영을 위해 이름, 이메일, 전화번호를 수집·이용하는 것에
                동의합니다.
              </span>
            </label>
            {submitError && <p className="ams-form__error">{submitError}</p>}
            <button
              type="submit"
              className="ams-btn ams-btn--primary ams-btn--block ams-form__submit"
              disabled={submitting}
            >
              {submitting ? '가입 중…' : '가입'}
            </button>
          </form>
          <div className="ams-card__footer">
            <Link to="/login">로그인으로 돌아가기</Link>
          </div>
        </div>
      )}
    </SignupInviteGate>
  )
}
