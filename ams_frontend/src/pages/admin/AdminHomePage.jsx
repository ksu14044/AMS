import { useCallback, useEffect, useState } from 'react'
import { approveStaff, fetchPendingStaff } from '../../api/adminApi'
import { STAFF_ROLES, roleLabel } from '../../auth/roleLabels'
import { SUBJECT_OPTIONS } from '../../auth/subjectLabels'
import AcademyNoticesSection from '../../components/AcademyNoticesSection'
import StudentRosterHomeCard from '../../components/StudentRosterHomeCard'
import AdminClassesSection from './AdminClassesSection'
import AdminSignupInvitesSection from './AdminSignupInvitesSection'
import '../../styles/admin.css'

function roleRequiresSubject(role) {
  return role?.startsWith('TEACHER_') || role?.startsWith('ASSISTANT_')
}

export default function AdminHomePage() {
  const [pending, setPending] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [approvingId, setApprovingId] = useState(null)
  const [forms, setForms] = useState({})

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const list = await fetchPendingStaff()
      setPending(list)
      const initial = {}
      for (const u of list) {
        initial[u.userId] = { role: u.role, subject: u.subject ?? '' }
      }
      setForms(initial)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    load()
  }, [load])

  function updateForm(userId, patch) {
    setForms((prev) => ({
      ...prev,
      [userId]: { ...prev[userId], ...patch },
    }))
  }

  async function handleApprove(userId) {
    const form = forms[userId]
    if (!form?.role) return
    if (roleRequiresSubject(form.role) && !form.subject) {
      setError('선생님·조교는 과목을 선택해 주세요.')
      return
    }
    setApprovingId(userId)
    setError('')
    try {
      await approveStaff(userId, {
        role: form.role,
        subject: roleRequiresSubject(form.role) ? form.subject : null,
      })
      await load()
    } catch (err) {
      setError(err.message)
    } finally {
      setApprovingId(null)
    }
  }

  return (
    <div className="ams-admin">
      <AdminSignupInvitesSection />

      <section className="ams-admin__section">
        <h2 className="ams-admin__heading">교직원 승인 대기</h2>
        <p className="ams-admin__desc">가입 신청한 교직원의 역할·과목을 확정한 뒤 승인합니다.</p>

        {error && <p className="ams-admin__error">{error}</p>}

        {loading ? (
          <p className="ams-admin__empty">불러오는 중…</p>
        ) : pending.length === 0 ? (
          <p className="ams-admin__empty">승인 대기 중인 교직원이 없습니다.</p>
        ) : (
          <ul className="ams-pending-list">
            {pending.map((u) => {
              const form = forms[u.userId] ?? {}
              const needsSubject = roleRequiresSubject(form.role)
              return (
                <li key={u.userId} className="ams-pending-item">
                  <div className="ams-pending-item__info">
                    <strong>{u.name}</strong>
                    <span className="ams-pending-item__email">{u.email}</span>
                    <span className="ams-pending-item__meta">
                      신청 역할: {roleLabel(u.role)}
                    </span>
                  </div>
                  <div className="ams-pending-item__form">
                    <label>
                      확정 역할
                      <select
                        value={form.role ?? u.role}
                        onChange={(e) => updateForm(u.userId, { role: e.target.value })}
                      >
                        {STAFF_ROLES.map((r) => (
                          <option key={r.value} value={r.value}>
                            {r.label}
                          </option>
                        ))}
                      </select>
                    </label>
                    {needsSubject && (
                      <label>
                        과목
                        <select
                          value={form.subject ?? ''}
                          onChange={(e) => updateForm(u.userId, { subject: e.target.value })}
                        >
                          <option value="">선택</option>
                          {SUBJECT_OPTIONS.map((s) => (
                            <option key={s.value} value={s.value}>
                              {s.label}
                            </option>
                          ))}
                        </select>
                      </label>
                    )}
                    <button
                      type="button"
                      className="ams-btn ams-btn--primary"
                      disabled={approvingId === u.userId}
                      onClick={() => handleApprove(u.userId)}
                    >
                      {approvingId === u.userId ? '처리 중…' : '승인'}
                    </button>
                  </div>
                </li>
              )
            })}
          </ul>
        )}
      </section>

      <AcademyNoticesSection canManage />

      <StudentRosterHomeCard />

      <AdminClassesSection />
    </div>
  )
}
