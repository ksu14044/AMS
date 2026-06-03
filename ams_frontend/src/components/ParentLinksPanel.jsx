import { useCallback, useEffect, useState } from 'react'
import { createParentLink, deleteParentLink, fetchParentLinksByStudent } from '../api/parentApi'

export default function ParentLinksPanel({ studentId, studentName }) {
  const [links, setLinks] = useState([])
  const [email, setEmail] = useState('')
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      setLinks(await fetchParentLinksByStudent(studentId))
    } catch (err) {
      setError(err.message)
      setLinks([])
    } finally {
      setLoading(false)
    }
  }, [studentId])

  useEffect(() => {
    load()
  }, [load])

  async function handleAdd(e) {
    e.preventDefault()
    if (!email.trim()) return
    setSubmitting(true)
    setError('')
    setMessage('')
    try {
      await createParentLink({ parentEmail: email.trim(), studentId })
      setEmail('')
      setMessage('학부모 연결을 추가했습니다.')
      await load()
    } catch (err) {
      setError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  async function handleRemove(linkId) {
    if (!window.confirm('이 학부모 연결을 해제할까요?')) return
    setError('')
    try {
      await deleteParentLink(linkId)
      await load()
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <div className="ams-parent-links ams-card ams-card--elevated">
      <h3 className="ams-class-detail__heading">학부모 연결</h3>
      <p className="ams-class-detail__hint-inline">
        {studentName} 학생과 연결된 학부모 계정(이메일)입니다. 학부모는 가입 후 조회만 가능합니다.
      </p>
      <form className="ams-parent-links__form" onSubmit={handleAdd}>
        <label className="ams-field ams-field--compact">
          <span className="ams-field__label">학부모 이메일</span>
          <input
            className="ams-field__input"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="parent@example.com"
            required
          />
        </label>
        <button type="submit" className="ams-btn ams-btn--primary ams-btn--sm" disabled={submitting}>
          {submitting ? '연결 중…' : '연결 추가'}
        </button>
      </form>
      {message && <p className="ams-class-detail__hint-inline">{message}</p>}
      {error && <p className="ams-form__error">{error}</p>}
      {loading ? (
        <p className="ams-class-detail__empty">불러오는 중…</p>
      ) : links.length === 0 ? (
        <p className="ams-class-detail__empty">연결된 학부모가 없습니다.</p>
      ) : (
        <ul className="ams-parent-links__list">
          {links.map((l) => (
            <li key={l.linkId} className="ams-parent-links__item">
              <span>
                <strong>{l.parentName}</strong> · {l.parentEmail}
              </span>
              <button
                type="button"
                className="ams-btn ams-btn--ghost ams-btn--sm"
                onClick={() => handleRemove(l.linkId)}
              >
                연결 해제
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
