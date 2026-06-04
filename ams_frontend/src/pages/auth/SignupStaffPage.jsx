import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import PhoneNumberField from '../../components/PhoneNumberField'
import { useAuth } from '../../auth/AuthContext'
import { STAFF_ROLES } from '../../auth/roleLabels'
import SignupInviteGate from '../../components/auth/SignupInviteGate'
import { useSignupInvite } from '../../hooks/useSignupInvite'

export default function SignupStaffPage() {
  const { signupStaff } = useAuth()
  const navigate = useNavigate()
  const { token, invite, loading, error } = useSignupInvite('STAFF')
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

  const selectedRole = STAFF_ROLES.find((r) => r.value === invite?.role)

  async function handleSubmit(e) {
    e.preventDefault()
    setSubmitError('')
    if (form.password !== form.confirmPassword) {
      setSubmitError('비밀번호와 비밀번호 확인이 일치하지 않습니다.')
      return
    }
    setSubmitting(true)
    try {
      await signupStaff({
        inviteToken: token,
        academyCode: invite.academyCode,
        name: form.name,
        email: form.email,
        password: form.password,
        phoneNumber: form.phoneNumber,
        personalInfoConsent: form.personalInfoConsent,
        role: invite.role,
        subject: selectedRole?.subject ?? null,
      })
      navigate('/pending')
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
          <h1 className="ams-card__title">교직원 가입</h1>
          <p className="ams-card__desc">
            {resolvedInvite.academyName && (
              <>
                <strong>{resolvedInvite.academyName}</strong>
                <br />
              </>
            )}
            {resolvedInvite.roleLabel} · 원장 승인 후 이용할 수 있습니다.
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
              <span>역할</span>
              <input value={resolvedInvite.roleLabel} readOnly disabled />
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
                본인은 AMS 서비스 회원가입과 학원 운영(인력 관리, 수업/과제/클리닉 운영, 본인 확인 및 고객 응대)을 위해 이름, 이메일,
                전화번호를 수집·이용하는 것에 동의합니다. 보유·이용 기간은 회원 탈퇴 후 최대 30일까지(관계 법령에 따라 별도 보관이 필요한
                경우 해당 기간까지)이며, 동의를 거부할 권리가 있으나 거부 시 회원가입이 제한될 수 있습니다.
              </span>
            </label>
            {submitError && <p className="ams-form__error">{submitError}</p>}
            <button
              type="submit"
              className="ams-btn ams-btn--primary ams-btn--block ams-form__submit"
              disabled={submitting}
            >
              {submitting ? '가입 중…' : '가입 신청'}
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
