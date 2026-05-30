import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  completeHomework,
  createHomework,
  fetchHomeworkAnswerKeys,
  fetchHomeworkSubmissions,
  fetchHomeworks,
  gradeHomeworkSubmission,
  saveHomeworkAnswerKeys,
} from '../../api/classesApi'

const STATUS_LABEL = { SCHEDULED: '예정', COMPLETED: '완료' }

function emptyAnswers(count) {
  return Array.from({ length: Math.max(count, 0) }, () => '')
}

function answersFromKeyResponse(data) {
  const count = data?.questionCount ?? 0
  const items = data?.items ?? []
  const answers = emptyAnswers(count)
  for (const item of items) {
    const idx = item.questionNo - 1
    if (idx >= 0 && idx < answers.length) {
      answers[idx] = item.correctAnswer ?? ''
    }
  }
  return answers
}

function answersFromRow(row, count) {
  if (Array.isArray(row?.answers) && row.answers.length > 0) {
    const normalized = emptyAnswers(count)
    for (let i = 0; i < count; i++) {
      normalized[i] = row.answers[i] ?? ''
    }
    return normalized
  }
  return emptyAnswers(count)
}

function answersEqual(a, b) {
  if (!a || !b || a.length !== b.length) return false
  return a.every((value, index) => (value ?? '') === (b[index] ?? ''))
}

function HomeworkModalBackdrop({ wide, label, onClose, children }) {
  return (
    <div className="ams-homework-modal-backdrop" onClick={onClose}>
      <div
        className={`ams-homework-modal${wide ? ' ams-homework-modal--wide' : ''}`}
        role="dialog"
        aria-modal="true"
        aria-label={label}
        onClick={(e) => e.stopPropagation()}
      >
        {children}
      </div>
    </div>
  )
}

function AnswerKeyModal({
  homeworkTitle,
  questionCount,
  answerKeyDraft,
  submitting,
  answerKeyDirty,
  onQuestionCountChange,
  onAnswerChange,
  onSave,
  onClose,
}) {
  return (
    <HomeworkModalBackdrop label="정답지 설정" onClose={onClose}>
      <header className="ams-homework-modal__header">
        <h4 className="ams-homework-modal__title">정답지 설정</h4>
        <button type="button" className="ams-homework-modal__close" onClick={onClose} aria-label="닫기">
          ×
        </button>
      </header>
      <p className="ams-homework-modal__meta">{homeworkTitle}</p>

      <form className="ams-homework-modal__body" onSubmit={onSave}>
        <label className="ams-homework-modal__count">
          문항 수
          <input
            type="number"
            min={1}
            value={questionCount || ''}
            disabled={submitting}
            onChange={(e) => onQuestionCountChange(e.target.value)}
          />
        </label>

        {questionCount > 0 && (
          <div className="ams-homework-modal__grid">
            {Array.from({ length: questionCount }, (_, i) => (
              <label key={i} className="ams-homework-modal__field">
                <span>{i + 1}번 정답</span>
                <input
                  type="text"
                  maxLength={500}
                  value={answerKeyDraft.answers[i] ?? ''}
                  disabled={submitting}
                  onChange={(e) => onAnswerChange(i, e.target.value)}
                />
              </label>
            ))}
          </div>
        )}

        <footer className="ams-homework-modal__footer">
          <button type="button" className="ams-btn ams-btn--ghost" disabled={submitting} onClick={onClose}>
            취소
          </button>
          <button
            type="submit"
            className="ams-btn ams-btn--primary"
            disabled={submitting || !answerKeyDirty || questionCount <= 0}
          >
            {submitting ? '저장 중…' : '정답지 저장'}
          </button>
        </footer>
      </form>
    </HomeworkModalBackdrop>
  )
}

function GradeModal({
  studentName,
  homeworkTitle,
  questionCount,
  correctAnswers,
  draft,
  canManage,
  grading,
  dirty,
  onDraftChange,
  onGrade,
  onClose,
}) {
  return (
    <HomeworkModalBackdrop wide label={`${studentName} 답안`} onClose={onClose}>
      <header className="ams-homework-modal__header">
        <h4 className="ams-homework-modal__title">{canManage ? '답안 입력·채점' : '내 답안'}</h4>
        <button type="button" className="ams-homework-modal__close" onClick={onClose} aria-label="닫기">
          ×
        </button>
      </header>
      <p className="ams-homework-modal__meta">
        {studentName} · {homeworkTitle}
      </p>

      <div className="ams-homework-modal__body">
        <table className="ams-homework-grade-table">
          <thead>
            <tr>
              <th>문항</th>
              {canManage && correctAnswers.length > 0 && <th>정답</th>}
              <th>{canManage ? '학생 답안' : '답안'}</th>
            </tr>
          </thead>
          <tbody>
            {Array.from({ length: questionCount }, (_, i) => (
              <tr key={i}>
                <td>{i + 1}번</td>
                {canManage && correctAnswers.length > 0 && (
                  <td className="ams-homework-grade-table__correct">{correctAnswers[i] || '—'}</td>
                )}
                <td>
                  {canManage ? (
                    <input
                      type="text"
                      className="ams-homework-grade-table__input"
                      maxLength={500}
                      value={draft[i] ?? ''}
                      disabled={grading}
                      onChange={(e) => onDraftChange(i, e.target.value)}
                    />
                  ) : (
                    <span>{draft[i]?.trim() ? draft[i] : '—'}</span>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <footer className="ams-homework-modal__footer">
        <button type="button" className="ams-btn ams-btn--ghost" disabled={grading} onClick={onClose}>
          {canManage ? '취소' : '닫기'}
        </button>
        {canManage && (
          <button
            type="button"
            className="ams-btn ams-btn--primary"
            disabled={grading || !dirty}
            onClick={onGrade}
          >
            {grading ? '채점 중…' : '채점'}
          </button>
        )}
      </footer>
    </HomeworkModalBackdrop>
  )
}

export default function ClassHomeworkSection({ classId, canManage, verifyOnly = false, onError }) {
  const [homeworks, setHomeworks] = useState([])
  const [selectedId, setSelectedId] = useState('')
  const [submissions, setSubmissions] = useState([])
  const [questionCount, setQuestionCount] = useState(0)
  const [answerKeyDraft, setAnswerKeyDraft] = useState({ questionCount: 0, answers: [] })
  const [savedAnswerKey, setSavedAnswerKey] = useState({ questionCount: 0, answers: [] })
  const [studentAnswerDraft, setStudentAnswerDraft] = useState({})
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [grading, setGrading] = useState(false)
  const [modal, setModal] = useState(null)
  const [gradeModalDraft, setGradeModalDraft] = useState([])
  const [form, setForm] = useState({ title: '', questionCount: '' })

  const selectedHomework = useMemo(
    () => homeworks.find((h) => String(h.homeworkId) === selectedId),
    [homeworks, selectedId],
  )

  const hasAnswerKey = savedAnswerKey.answers.some((a) => a.trim() !== '')

  const gradingStudent = useMemo(() => {
    if (modal?.type !== 'grade') return null
    return submissions.find((s) => s.studentId === modal.studentId) ?? null
  }, [modal, submissions])

  const gradeModalDirty = useMemo(() => {
    if (!gradingStudent) return false
    const saved = answersFromRow(gradingStudent, questionCount)
    return !answersEqual(saved, gradeModalDraft)
  }, [gradingStudent, questionCount, gradeModalDraft])

  const loadHomeworks = useCallback(async () => {
    const list = await fetchHomeworks(classId)
    setHomeworks(list)
    if (list.length > 0 && !selectedId) {
      setSelectedId(String(list[0].homeworkId))
    }
  }, [classId, selectedId])

  const loadAnswerKeys = useCallback(async () => {
    if (!selectedId) {
      setQuestionCount(0)
      setAnswerKeyDraft({ questionCount: 0, answers: [] })
      setSavedAnswerKey({ questionCount: 0, answers: [] })
      return
    }
    const data = await fetchHomeworkAnswerKeys(classId, selectedId)
    const count = data.questionCount || selectedHomework?.questionCount || 0
    const answers = answersFromKeyResponse(data)
    setQuestionCount(count)
    setAnswerKeyDraft({ questionCount: count, answers: [...answers] })
    setSavedAnswerKey({ questionCount: count, answers: [...answers] })
  }, [classId, selectedId, selectedHomework?.questionCount])

  const loadSubmissions = useCallback(async () => {
    if (!selectedId) {
      setSubmissions([])
      setStudentAnswerDraft({})
      return
    }
    const rows = await fetchHomeworkSubmissions(classId, selectedId)
    setSubmissions(rows)
    const count = questionCount || selectedHomework?.questionCount || 0
    const draft = {}
    for (const row of rows) {
      draft[row.studentId] = answersFromRow(row, count)
    }
    setStudentAnswerDraft(draft)
  }, [classId, selectedId, questionCount, selectedHomework?.questionCount])

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
    ;(async () => {
      onError('')
      try {
        await loadAnswerKeys()
      } catch (err) {
        onError(err.message)
      }
    })()
  }, [loadAnswerKeys, onError])

  useEffect(() => {
    loadSubmissions().catch((err) => onError(err.message))
  }, [loadSubmissions, onError])

  useEffect(() => {
    setModal(null)
    setGradeModalDraft([])
  }, [selectedId])

  function resetAnswerKeyDraft() {
    setQuestionCount(savedAnswerKey.questionCount)
    setAnswerKeyDraft({
      questionCount: savedAnswerKey.questionCount,
      answers: [...savedAnswerKey.answers],
    })
  }

  function openAnswerKeyModal() {
    resetAnswerKeyDraft()
    setModal({ type: 'answer-key' })
  }

  function closeAnswerKeyModal() {
    resetAnswerKeyDraft()
    setModal(null)
  }

  function openGradeModal(studentId) {
    const answers = studentAnswerDraft[studentId] ?? emptyAnswers(questionCount)
    setGradeModalDraft([...answers])
    setModal({ type: 'grade', studentId })
  }

  function closeGradeModal() {
    setModal(null)
    setGradeModalDraft([])
  }

  function handleQuestionCountChange(value) {
    const count = Math.max(0, Number(value) || 0)
    setQuestionCount(count)
    setAnswerKeyDraft((prev) => ({
      questionCount: count,
      answers: emptyAnswers(count).map((_, i) => prev.answers[i] ?? ''),
    }))
  }

  function handleAnswerKeyChange(index, value) {
    setAnswerKeyDraft((prev) => {
      const answers = [...prev.answers]
      answers[index] = value
      return { ...prev, answers }
    })
  }

  async function handleSaveAnswerKey(e) {
    e.preventDefault()
    if (questionCount <= 0) {
      onError('문항 수를 1 이상 입력하세요.')
      return
    }
    setSubmitting(true)
    onError('')
    try {
      const saved = await saveHomeworkAnswerKeys(classId, selectedId, {
        questionCount,
        answers: answerKeyDraft.answers,
      })
      const answers = answersFromKeyResponse(saved)
      setQuestionCount(saved.questionCount)
      setAnswerKeyDraft({ questionCount: saved.questionCount, answers: [...answers] })
      setSavedAnswerKey({ questionCount: saved.questionCount, answers: [...answers] })
      await loadHomeworks()
      await loadSubmissions()
      setModal(null)
    } catch (err) {
      onError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  async function handleGradeFromModal() {
    if (!gradingStudent || !hasAnswerKey) return
    const studentId = gradingStudent.studentId
    const saved = answersFromRow(gradingStudent, questionCount)
    if (answersEqual(saved, gradeModalDraft)) return

    setGrading(true)
    onError('')
    try {
      const updated = await gradeHomeworkSubmission(classId, selectedId, studentId, {
        answers: gradeModalDraft,
      })
      setSubmissions((prev) => prev.map((s) => (s.studentId === studentId ? updated : s)))
      setStudentAnswerDraft((prev) => ({
        ...prev,
        [studentId]: answersFromRow(updated, questionCount),
      }))
      closeGradeModal()
    } catch (err) {
      onError(err.message)
    } finally {
      setGrading(false)
    }
  }

  async function handleCreate(e) {
    e.preventDefault()
    if (!form.title.trim()) return
    setSubmitting(true)
    onError('')
    try {
      const created = await createHomework(classId, {
        title: form.title.trim(),
        questionCount: form.questionCount ? Number(form.questionCount) : null,
      })
      setForm({ title: '', questionCount: '' })
      await loadHomeworks()
      setSelectedId(String(created.homeworkId))
    } catch (err) {
      onError(err.message)
    } finally {
      setSubmitting(false)
    }
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

  const answerKeyDirty =
    questionCount !== savedAnswerKey.questionCount ||
    !answersEqual(answerKeyDraft.answers, savedAnswerKey.answers)

  if (loading) {
    return <p className="ams-class-detail__empty">불러오는 중…</p>
  }

  return (
    <section className="ams-class-detail__section">
      <h3 className="ams-class-detail__heading">{verifyOnly ? '숙제 확인' : '숙제'}</h3>
      <p className="ams-class-detail__hint-inline">
        {verifyOnly
          ? '정답지를 설정한 뒤 학생별 「답안 입력」에서 답을 입력하고 채점하세요.'
          : '교직원이 정답지·학생 답안을 모달에서 입력합니다.'}
      </p>

      {canManage && !verifyOnly && (
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
            문항 수
            <input
              type="number"
              min={1}
              value={form.questionCount}
              onChange={(e) => setForm({ ...form, questionCount: e.target.value })}
              placeholder="정오표 설정 시 입력"
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
                  [{STATUS_LABEL[h.status] || h.status}] {h.title}
                  {h.questionCount ? ` · ${h.questionCount}문항` : ''}
                </option>
              ))}
            </select>
          </label>

          {selectedId && (
            <div className="ams-homework-toolbar">
              {canManage && (
                <>
                  <button
                    type="button"
                    className="ams-btn ams-btn--primary"
                    onClick={openAnswerKeyModal}
                  >
                    정답지 설정
                    {hasAnswerKey && (
                      <span className="ams-homework-toolbar__badge">등록됨</span>
                    )}
                  </button>
                  <button
                    type="button"
                    className="ams-btn ams-btn--ghost"
                    disabled={submitting}
                    onClick={handleComplete}
                  >
                    완료 처리
                  </button>
                </>
              )}
              {!canManage && !hasAnswerKey && questionCount <= 0 && (
                <p className="ams-class-detail__empty">아직 정답지가 등록되지 않았습니다.</p>
              )}
            </div>
          )}

          {submissions.length > 0 && (
            <div className="ams-submission-table-wrap">
              <table className="ams-submission-table ams-submission-table--homework">
                <thead>
                  <tr>
                    <th>학생</th>
                    <th>맞은 수</th>
                    <th>점수</th>
                    <th>상태</th>
                    <th>{canManage ? '답안' : '확인'}</th>
                  </tr>
                </thead>
                <tbody>
                  {submissions.map((s) => (
                    <tr key={s.studentId}>
                      <td>{s.studentName}</td>
                      <td>
                        {s.correctCount != null ? `${s.correctCount}/${questionCount || '?'}` : '—'}
                      </td>
                      <td>{s.score != null ? `${s.score}점` : '—'}</td>
                      <td>{s.completedAt ? '완료' : hasAnswerKey ? '미채점' : '—'}</td>
                      <td>
                        {(canManage && hasAnswerKey) || (!canManage && s.completedAt) ? (
                          <button
                            type="button"
                            className="ams-btn ams-btn--ghost ams-homework-row-btn"
                            onClick={() => openGradeModal(s.studentId)}
                          >
                            {canManage ? (s.completedAt ? '수정' : '답안 입력') : '보기'}
                          </button>
                        ) : (
                          '—'
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}

      {modal?.type === 'answer-key' && canManage && (
        <AnswerKeyModal
          homeworkTitle={selectedHomework?.title ?? ''}
          questionCount={questionCount}
          answerKeyDraft={answerKeyDraft}
          submitting={submitting}
          answerKeyDirty={answerKeyDirty}
          onQuestionCountChange={handleQuestionCountChange}
          onAnswerChange={handleAnswerKeyChange}
          onSave={handleSaveAnswerKey}
          onClose={closeAnswerKeyModal}
        />
      )}

      {modal?.type === 'grade' && gradingStudent && questionCount > 0 && (
        <GradeModal
          studentName={gradingStudent.studentName}
          homeworkTitle={selectedHomework?.title ?? ''}
          questionCount={questionCount}
          correctAnswers={canManage ? savedAnswerKey.answers : []}
          draft={gradeModalDraft}
          canManage={canManage && hasAnswerKey}
          grading={grading}
          dirty={gradeModalDirty}
          onDraftChange={(index, value) => {
            setGradeModalDraft((prev) => {
              const next = [...prev]
              next[index] = value
              return next
            })
          }}
          onGrade={handleGradeFromModal}
          onClose={closeGradeModal}
        />
      )}
    </section>
  )
}
