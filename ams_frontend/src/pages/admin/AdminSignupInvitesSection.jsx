import { useState } from 'react'
import { createSignupInvite } from '../../api/adminApi'
import { STAFF_ROLES } from '../../auth/roleLabels'

async function copyToClipboard(text) {
  try {
    await navigator.clipboard.writeText(text)
    return true
  } catch {
    return false
  }
}

function formatExpiresAt(iso) {
  if (!iso) return ''
  return new Date(iso).toLocaleString('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export default function AdminSignupInvitesSection() {
  const [studentUrl, setStudentUrl] = useState('')
  const [studentExpiresAt, setStudentExpiresAt] = useState('')
  const [staffRole, setStaffRole] = useState('TEACHER_KO')
  const [staffUrl, setStaffUrl] = useState('')
  const [staffExpiresAt, setStaffExpiresAt] = useState('')
  const [loadingKind, setLoadingKind] = useState(null)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')

  const staffRoleLabel = STAFF_ROLES.find((r) => r.value === staffRole)?.label ?? ''

  async function issueStudentLink() {
    setLoadingKind('STUDENT')
    setError('')
    setMessage('')
    try {
      const result = await createSignupInvite({ kind: 'STUDENT' })
      setStudentUrl(result.url)
      setStudentExpiresAt(result.expiresAt)
      setMessage('학생 가입 링크가 생성되었습니다.')
    } catch (err) {
      setError(err.message)
    } finally {
      setLoadingKind(null)
    }
  }

  async function issueStaffLink() {
    setLoadingKind('STAFF')
    setError('')
    setMessage('')
    try {
      const result = await createSignupInvite({ kind: 'STAFF', role: staffRole })
      setStaffUrl(result.url)
      setStaffExpiresAt(result.expiresAt)
      setMessage(`${result.roleLabel} 가입 링크가 생성되었습니다.`)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoadingKind(null)
    }
  }

  async function handleCopy(url, label) {
    const ok = await copyToClipboard(url)
    setMessage(ok ? `${label} 링크를 복사했습니다.` : '복사에 실패했습니다. 링크를 직접 선택해 복사해 주세요.')
  }

  return (
    <section className="ams-admin__section">
      <h2 className="ams-admin__heading">회원가입 링크</h2>
      <p className="ams-admin__desc">
        역할별 전용 링크를 생성해 카카오·문자·이메일 등으로 전송하세요. 링크는 발급 후{' '}
        <strong>7일간</strong> 유효하며, 만료 전까지 여러 명이 같은 링크로 가입할 수 있습니다.
      </p>

      {error && <p className="ams-admin__error">{error}</p>}
      {message && <p className="ams-admin__hint">{message}</p>}

      <div className="ams-signup-invites">
        <div className="ams-signup-invites__block">
          <h3 className="ams-signup-invites__title">학생</h3>
          <button
            type="button"
            className="ams-btn ams-btn--primary"
            disabled={loadingKind === 'STUDENT'}
            onClick={issueStudentLink}
          >
            {loadingKind === 'STUDENT' ? '생성 중…' : '학생 가입 링크 생성'}
          </button>
          {studentUrl && (
            <>
              <p className="ams-signup-invites__expiry">
                만료: {formatExpiresAt(studentExpiresAt)}까지
              </p>
              <div className="ams-signup-invites__url">
                <input type="text" readOnly value={studentUrl} />
                <button
                  type="button"
                  className="ams-btn ams-btn--ghost-sm"
                  onClick={() => handleCopy(studentUrl, '학생')}
                >
                  복사
                </button>
              </div>
            </>
          )}
        </div>

        <div className="ams-signup-invites__block">
          <h3 className="ams-signup-invites__title">교직원</h3>
          <div className="ams-signup-invites__form">
            <label className="ams-signup-invites__field">
              <span>역할</span>
              <select
                value={staffRole}
                onChange={(e) => {
                  setStaffRole(e.target.value)
                  setStaffUrl('')
                  setStaffExpiresAt('')
                }}
              >
                {STAFF_ROLES.map((r) => (
                  <option key={r.value} value={r.value}>
                    {r.label}
                  </option>
                ))}
              </select>
            </label>
            <button
              type="button"
              className="ams-btn ams-btn--primary"
              disabled={loadingKind === 'STAFF'}
              onClick={issueStaffLink}
            >
              {loadingKind === 'STAFF' ? '생성 중…' : '가입 링크 생성'}
            </button>
          </div>
          {staffUrl && (
            <>
              <p className="ams-signup-invites__expiry">만료: {formatExpiresAt(staffExpiresAt)}까지</p>
              <div className="ams-signup-invites__url">
                <input type="text" readOnly value={staffUrl} />
                <button
                  type="button"
                  className="ams-btn ams-btn--ghost-sm"
                  onClick={() => handleCopy(staffUrl, staffRoleLabel)}
                >
                  복사
                </button>
              </div>
            </>
          )}
        </div>
      </div>
    </section>
  )
}
