import { useCallback, useEffect, useState } from 'react'
import {
  completeHomework,
  createHomework,
  fetchHomeworkSubmissions,
  fetchHomeworks,
  toInstant,
  updateHomeworkSubmission,
} from '../../api/classesApi'

const STATUS_LABEL = { SCHEDULED: '예정', COMPLETED: '완료' }

function draftFromRow(row) {
  return {
    submitted: row.submitted,
    score: row.score ?? '',
    grade: row.grade ?? '',
  }
}

function parseDraftScore(value) {
  if (value === '' || value == null) return null
  const n = Number(value)
  return Number.isNaN(n) ? null : n
}

function isDraftDirty(row, draft) {
  if (!row || !draft) return false
  return (
    row.submitted !== draft.submitted ||
    row.score !== parseDraftScore(draft.score) ||
    (row.grade ?? null) !== (draft.grade || null)
  )
}

export default function ClassHomeworkSection({ classId, canManage, onError }) {
  const [homeworks, setHomeworks] = useState([])
  const [selectedId, setSelectedId] = useState('')
  const [submissions, setSubmissions] = useState([])
  const [submissionDraft, setSubmissionDraft] = useState({})
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [form, setForm] = useState({ title: '', dueDate: '', dueTime: '18:00' })

  const loadHomeworks = useCallback(async () => {
    const list = await fetchHomeworks(classId)
    setHomeworks(list)
    if (list.length > 0 && !selectedId) {
      setSelectedId(String(list[0].homeworkId))
    }
  }, [classId, selectedId])

  const loadSubmissions = useCallback(async () => {
    if (!selectedId) {
      setSubmissions([])
      setSubmissionDraft({})
      return
    }
    const rows = await fetchHomeworkSubmissions(classId, selectedId)
    setSubmissions(rows)
    const draft = {}
    for (const row of rows) {
      draft[row.studentId] = draftFromRow(row)
    }
    setSubmissionDraft(draft)
  }, [classId, selectedId])

  useEffect(() => {
    ;(async () => {
      setLoading(true)
      onError('')
      try {
        await loadHomeworks()
      } catch (err) {
        onError(err.message)
      } finally {
        setLoading(false)
      }
    })()
  }, [loadHomeworks, onError])

  useEffect(() => {
    loadSubmissions().catch((err) => onError(err.message))
  }, [loadSubmissions, onError])

  async function handleCreate(e) {
    e.preventDefault()
    if (!form.title.trim() || !form.dueDate) return
    setSubmitting(true)
    onError('')
    try {
      const created = await createHomework(classId, {
        title: form.title.trim(),
        dueAt: toInstant(form.dueDate, form.dueTime),
      })
      setForm({ title: '', dueDate: '', dueTime: '18:00' })
      await loadHomeworks()
      setSelectedId(String(created.homeworkId))
    } catch (err) {
      onError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  async function persistStudent(studentId, draftOverride) {
    const row = submissions.find((s) => s.studentId === studentId)
    const draft = draftOverride ?? submissionDraft[studentId]
    if (!row || !draft || !isDraftDirty(row, draft)) {
      return
    }

    setSubmitting(true)
    onError('')
    try {
      const updated = await updateHomeworkSubmission(classId, selectedId, studentId, {
        submitted: draft.submitted,
        submittedAt: null,
        score: parseDraftScore(draft.score),
        grade: draft.grade || null,
        memo: row.memo ?? null,
      })
      setSubmissions((prev) => prev.map((s) => (s.studentId === studentId ? updated : s)))
      setSubmissionDraft((prev) => ({
        ...prev,
        [studentId]: draftFromRow(updated),
      }))
    } catch (err) {
      onError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  function handleSubmittedChange(studentId, submitted) {
    const nextDraft = { ...submissionDraft[studentId], submitted }
    setSubmissionDraft((prev) => ({ ...prev, [studentId]: nextDraft }))
    persistStudent(studentId, nextDraft)
  }

  async function handleComplete() {
    setSubmitting(true)
    onError('')
    try {
      await completeHomework(classId, selectedId)
      await loadHomeworks()
    } catch (err) {
      onError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) {
    return <p className="ams-class-detail__empty">불러오는 중…</p>
  }

  return (
    <section className="ams-class-detail__section">
      <h3 className="ams-class-detail__heading">숙제</h3>
      <p className="ams-class-detail__hint-inline">
        교사·관리자가 학생별 제출 여부·점수를 입력합니다. 점수·등급은 입력 후 다른 칸으로
        이동하면 저장됩니다.
      </p>

      {canManage && (
        <form className="ams-assignment-form" onSubmit={handleCreate}>
          <label>
            제목
            <input
              value={form.title}
              onChange={(e) => setForm({ ...form, title: e.target.value })}
              maxLength={200}
              required
            />
          </label>
          <label>
            마감일
            <input
              type="date"
              value={form.dueDate}
              onChange={(e) => setForm({ ...form, dueDate: e.target.value })}
              required
            />
          </label>
          <label>
            마감 시각
            <input
              type="time"
              value={form.dueTime}
              onChange={(e) => setForm({ ...form, dueTime: e.target.value })}
            />
          </label>
          <button type="submit" className="ams-btn ams-btn--primary" disabled={submitting}>
            숙제 등록
          </button>
        </form>
      )}

      {homeworks.length === 0 ? (
        <p className="ams-class-detail__empty">등록된 숙제가 없습니다.</p>
      ) : (
        <>
          <label className="ams-assignment-form__full">
            숙제 선택
            <select value={selectedId} onChange={(e) => setSelectedId(e.target.value)}>
              {homeworks.map((h) => (
                <option key={h.homeworkId} value={h.homeworkId}>
                  [{STATUS_LABEL[h.status] || h.status}] {h.title} —{' '}
                  {new Date(h.dueAt).toLocaleString('ko-KR')}
                </option>
              ))}
            </select>
          </label>

          {canManage && selectedId && (
            <button
              type="button"
              className="ams-btn ams-btn--ghost"
              disabled={submitting}
              onClick={handleComplete}
            >
              이 숙제를 완료 처리
            </button>
          )}

          <div className="ams-submission-table-wrap">
            <table className="ams-submission-table">
              <thead>
                <tr>
                  <th>학생</th>
                  <th>제출</th>
                  {canManage && <th>점수</th>}
                  {canManage && <th>등급</th>}
                  {!canManage && <th>상태</th>}
                </tr>
              </thead>
              <tbody>
                {submissions.map((s) => (
                  <tr key={s.studentId}>
                    <td>{s.studentName}</td>
                    <td>
                      {canManage ? (
                        <input
                          type="checkbox"
                          checked={submissionDraft[s.studentId]?.submitted ?? false}
                          disabled={submitting}
                          onChange={(e) => handleSubmittedChange(s.studentId, e.target.checked)}
                        />
                      ) : (
                        <span>{s.submitted ? '제출 완료' : '미제출'}</span>
                      )}
                    </td>
                    {canManage ? (
                      <>
                        <td>
                          <input
                            type="number"
                            className="ams-submission-table__num"
                            value={submissionDraft[s.studentId]?.score ?? ''}
                            disabled={submitting}
                            onChange={(e) =>
                              setSubmissionDraft((prev) => ({
                                ...prev,
                                [s.studentId]: { ...prev[s.studentId], score: e.target.value },
                              }))
                            }
                            onBlur={() => persistStudent(s.studentId)}
                          />
                        </td>
                        <td>
                          <input
                            type="text"
                            className="ams-submission-table__grade"
                            value={submissionDraft[s.studentId]?.grade ?? ''}
                            maxLength={16}
                            disabled={submitting}
                            onChange={(e) =>
                              setSubmissionDraft((prev) => ({
                                ...prev,
                                [s.studentId]: { ...prev[s.studentId], grade: e.target.value },
                              }))
                            }
                            onBlur={() => persistStudent(s.studentId)}
                          />
                        </td>
                      </>
                    ) : (
                      <td colSpan={2}>
                        {s.submitted ? '제출 완료' : '—'}
                        {s.score != null ? ` · ${s.score}점` : ''}
                        {s.grade ? ` (${s.grade})` : ''}
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
    </section>
  )
}
