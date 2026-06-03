import { useState } from 'react'
import { createClassNotice, fetchClassNotices } from '../../api/classesApi'
import ClassScheduleSection from './ClassScheduleSection'
import ClassTextbookSection from './ClassTextbookSection'

export default function ClassNoticesSection({ classId, canManage, notices, onNoticesChange, onError }) {
  const [submitting, setSubmitting] = useState(false)
  const [noticeForm, setNoticeForm] = useState({ title: '', body: '', attachmentUrl: '' })

  async function handleCreateNotice(e) {
    e.preventDefault()
    if (!noticeForm.title.trim() || !noticeForm.body.trim()) return
    setSubmitting(true)
    onError('')
    try {
      await createClassNotice(classId, {
        title: noticeForm.title.trim(),
        body: noticeForm.body.trim(),
        attachmentUrl: noticeForm.attachmentUrl.trim() || null,
      })
      setNoticeForm({ title: '', body: '', attachmentUrl: '' })
      onNoticesChange(await fetchClassNotices(classId))
    } catch (err) {
      onError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="ams-class-detail__notices-hub">
      <section className="ams-class-detail__section">
      <h3 className="ams-class-detail__heading">반 공지</h3>
      <p className="ams-class-detail__hint-inline">
        반 공지와 함께 수업 정보·교재를 확인·등록할 수 있습니다.
      </p>

      {canManage && (
        <form className="ams-notice-form" onSubmit={handleCreateNotice}>
          <label>
            제목
            <input
              type="text"
              value={noticeForm.title}
              onChange={(e) => setNoticeForm({ ...noticeForm, title: e.target.value })}
              maxLength={200}
              required
            />
          </label>
          <label>
            내용
            <textarea
              value={noticeForm.body}
              onChange={(e) => setNoticeForm({ ...noticeForm, body: e.target.value })}
              rows={4}
              required
            />
          </label>
          <label>
            첨부 URL (선택)
            <input
              type="url"
              value={noticeForm.attachmentUrl}
              onChange={(e) => setNoticeForm({ ...noticeForm, attachmentUrl: e.target.value })}
              placeholder="https://"
            />
          </label>
          <button type="submit" className="ams-btn ams-btn--primary" disabled={submitting}>
            {submitting ? '등록 중…' : '공지 등록'}
          </button>
        </form>
      )}

      {notices.length === 0 ? (
        <p className="ams-class-detail__empty">등록된 공지가 없습니다.</p>
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

      <ClassScheduleSection classId={classId} canManage={canManage} onError={onError} embedded />
      <ClassTextbookSection classId={classId} canManage={canManage} onError={onError} embedded />
    </div>
  )
}
