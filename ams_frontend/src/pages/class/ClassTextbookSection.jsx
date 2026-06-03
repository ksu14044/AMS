import { useCallback, useEffect, useState } from 'react'
import { fetchClassTextbook, updateClassTextbook } from '../../api/classesApi'

const EMPTY_FORM = { title: '', publisher: '', progressNote: '' }

export default function ClassTextbookSection({ classId, canManage, onError, embedded = false }) {
  const [data, setData] = useState(null)
  const [form, setForm] = useState(EMPTY_FORM)
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    onError('')
    try {
      const tb = await fetchClassTextbook(classId)
      setData(tb)
      setForm({
        title: tb.title ?? '',
        publisher: tb.publisher ?? '',
        progressNote: tb.progressNote ?? '',
      })
    } catch (err) {
      onError(err.message)
    } finally {
      setLoading(false)
    }
  }, [classId, onError])

  useEffect(() => {
    load()
  }, [load])

  async function handleSave(e) {
    e.preventDefault()
    if (!form.title.trim()) return
    setSubmitting(true)
    onError('')
    try {
      const updated = await updateClassTextbook(classId, {
        title: form.title.trim(),
        publisher: form.publisher.trim() || null,
        progressNote: form.progressNote.trim() || null,
      })
      setData(updated)
      setForm({
        title: updated.title ?? '',
        publisher: updated.publisher ?? '',
        progressNote: updated.progressNote ?? '',
      })
    } catch (err) {
      onError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) {
    return (
      <p className={embedded ? 'ams-class-detail__subsection-empty' : 'ams-class-detail__empty'}>
        불러오는 중…
      </p>
    )
  }

  const Wrapper = embedded ? 'div' : 'section'
  const headingTag = embedded ? 'h4' : 'h3'
  const Heading = headingTag

  return (
    <Wrapper className={embedded ? 'ams-class-detail__subsection' : 'ams-class-detail__section'}>
      <Heading className={embedded ? 'ams-class-detail__subheading' : 'ams-class-detail__heading'}>
        교재
      </Heading>
      <p className="ams-class-detail__hint-inline">
        담임·관리자만 수정합니다. 조교·학생은 조회만 가능합니다.
      </p>

      {!canManage && !data?.title && (
        <p className="ams-class-detail__empty">등록된 교재 정보가 없습니다.</p>
      )}

      {!canManage && data?.title && (
        <dl className="ams-textbook-view">
          <div>
            <dt>제목</dt>
            <dd>{data.title}</dd>
          </div>
          {data.publisher && (
            <div>
              <dt>출판사</dt>
              <dd>{data.publisher}</dd>
            </div>
          )}
          {data.progressNote && (
            <div>
              <dt>현재 진도</dt>
              <dd className="ams-textbook-view__progress">{data.progressNote}</dd>
            </div>
          )}
          {data.updatedAt && (
            <div>
              <dt>수정일</dt>
              <dd>{new Date(data.updatedAt).toLocaleString('ko-KR')}</dd>
            </div>
          )}
        </dl>
      )}

      {canManage && (
        <form className="ams-textbook-form" onSubmit={handleSave}>
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
          <label>
            출판사
            <input
              type="text"
              value={form.publisher}
              onChange={(e) => setForm({ ...form, publisher: e.target.value })}
              maxLength={100}
            />
          </label>
          <label>
            현재 진도
            <textarea
              value={form.progressNote}
              onChange={(e) => setForm({ ...form, progressNote: e.target.value })}
              rows={4}
              placeholder="예: 3단원 문법 ~ p.120"
            />
          </label>
          <button type="submit" className="ams-btn ams-btn--primary" disabled={submitting}>
            {submitting ? '저장 중…' : '교재 정보 저장'}
          </button>
        </form>
      )}
    </Wrapper>
  )
}
