import { useCallback, useEffect, useState } from 'react'
import { createAcademyNotice, fetchAcademyNotices } from '../api/academyApi'
import '../styles/class-detail.css'

export default function AcademyNoticesSection({ canManage, compact }) {
  const [notices, setNotices] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [form, setForm] = useState({ title: '', body: '', attachmentUrl: '' })

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      setNotices(await fetchAcademyNotices())
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    load()
  }, [load])

  async function handleCreate(e) {
    e.preventDefault()
    if (!form.title.trim() || !form.body.trim()) return
    setSubmitting(true)
    setError('')
    try {
      await createAcademyNotice({
        title: form.title.trim(),
        body: form.body.trim(),
        attachmentUrl: form.attachmentUrl.trim() || null,
      })
      setForm({ title: '', body: '', attachmentUrl: '' })
      await load()
    } catch (err) {
      setError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section
      className={
        compact
          ? 'ams-academy-notices ams-academy-notices--compact'
          : 'ams-academy-notices'
      }
    >
      <h2 className="ams-academy-notices__heading">학원 공지</h2>
      {!compact && (
        <p className="ams-class-detail__hint-inline">
          학원 전체에 게시됩니다. 행정·관리자만 등록할 수 있습니다.
        </p>
      )}

      {error && <p className="ams-class-detail__error">{error}</p>}

      {canManage && (
        <form className="ams-notice-form" onSubmit={handleCreate}>
          <label>
            제목
            <input
              type="text"
              value={form.title}
              onChange={(e) => setForm({ ...form, title: e.target.value })}
              maxLength={200}
              required
            />
          </label>
          <label className="ams-notice-form__full">
            내용
            <textarea
              value={form.body}
              onChange={(e) => setForm({ ...form, body: e.target.value })}
              rows={4}
              required
            />
          </label>
          <label>
            첨부 URL (선택)
            <input
              type="url"
              value={form.attachmentUrl}
              onChange={(e) => setForm({ ...form, attachmentUrl: e.target.value })}
              placeholder="https://"
            />
          </label>
          <button
            type="submit"
            className="ams-btn ams-btn--primary ams-btn--sm"
            disabled={submitting}
          >
            {submitting ? '등록 중…' : '학원 공지 등록'}
          </button>
        </form>
      )}

      {loading ? (
        <p className="ams-class-detail__empty">불러오는 중…</p>
      ) : notices.length === 0 ? (
        <p className="ams-class-detail__empty">등록된 학원 공지가 없습니다.</p>
      ) : (
        <ul className="ams-notice-list">
          {notices.map((n) => (
            <li key={n.noticeId} className="ams-notice-list__item">
              <h4 className="ams-notice-list__title">{n.title}</h4>
              <time className="ams-notice-list__date">
                {new Date(n.publishedAt).toLocaleString('ko-KR')}
              </time>
              <p className="ams-notice-list__body">{n.body}</p>
              {n.attachmentUrl && (
                <a
                  href={n.attachmentUrl}
                  className="ams-notice-list__link"
                  target="_blank"
                  rel="noreferrer"
                >
                  첨부 보기
                </a>
              )}
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
