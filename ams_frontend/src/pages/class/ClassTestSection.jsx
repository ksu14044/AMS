import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  completeTest,
  createTest,
  createTestRetake,
  fetchTestAnswerKeys,
  fetchTestScores,
  fetchTests,
  gradeTestScore,
  saveTestAnswerKeys,
  toInstant,
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

function TestModalBackdrop({ wide, label, onClose, children }) {
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
  testTitle,
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
    <TestModalBackdrop label="정답지 설정" onClose={onClose}>
      <header className="ams-homework-modal__header">
        <h4 className="ams-homework-modal__title">정답지 설정</h4>
        <button type="button" className="ams-homework-modal__close" onClick={onClose} aria-label="닫기">
          ×
        </button>
      </header>
      <p className="ams-homework-modal__meta">{testTitle}</p>

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
    </TestModalBackdrop>
  )
}

function GradeModal({
  studentName,
  testTitle,
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
    <TestModalBackdrop wide label={`${studentName} 답안`} onClose={onClose}>
      <header className="ams-homework-modal__header">
        <h4 className="ams-homework-modal__title">{canManage ? '답안 입력·채점' : '내 답안'}</h4>
        <button type="button" className="ams-homework-modal__close" onClick={onClose} aria-label="닫기">
          ×
        </button>
      </header>
      <p className="ams-homework-modal__meta">
        {studentName} · {testTitle}
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
    </TestModalBackdrop>
  )
}

function retakeStatusLabel(row) {
  if (row.needsRetake == null) return '—'
  return row.needsRetake ? '재시험 대상' : '합격'
}

export default function ClassTestSection({ classId, canManage, verifyOnly = false, onError }) {
  const [tests, setTests] = useState([])
  const [selectedId, setSelectedId] = useState('')
  const [scores, setScores] = useState([])
  const [questionCount, setQuestionCount] = useState(0)
  const [answerKeyDraft, setAnswerKeyDraft] = useState({ questionCount: 0, answers: [] })
  const [savedAnswerKey, setSavedAnswerKey] = useState({ questionCount: 0, answers: [] })
  const [studentAnswerDraft, setStudentAnswerDraft] = useState({})
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [grading, setGrading] = useState(false)
  const [modal, setModal] = useState(null)
  const [gradeModalDraft, setGradeModalDraft] = useState([])
  const [retakeOpen, setRetakeOpen] = useState(false)
  const [retakeForm, setRetakeForm] = useState({ testDate: '', testTime: '14:00' })
  const [form, setForm] = useState({
    title: '',
    testDate: '',
    testTime: '14:00',
    questionCount: '',
    retakeThresholdCount: '',
  })

  const selectedTest = useMemo(
    () => tests.find((t) => String(t.testId) === selectedId),
    [tests, selectedId],
  )

  const rootTest = selectedTest
    ? tests.find((t) => t.testId === selectedTest.rootTestId) ?? selectedTest
    : null

  const retakeCount = rootTest
    ? tests.filter((t) => t.parentTestId === rootTest.testId).length
    : 0

  const hasAnswerKey = savedAnswerKey.answers.some((a) => a.trim() !== '')

  const gradingStudent = useMemo(() => {
    if (modal?.type !== 'grade') return null
    return scores.find((s) => s.studentId === modal.studentId) ?? null
  }, [modal, scores])

  const gradeModalDirty = useMemo(() => {
    if (!gradingStudent) return false
    const saved = answersFromRow(gradingStudent, questionCount)
    return !answersEqual(saved, gradeModalDraft)
  }, [gradingStudent, questionCount, gradeModalDraft])

  const canScheduleRetake =
    canManage &&
    selectedTest?.status === 'COMPLETED' &&
    rootTest?.questionCount &&
    rootTest?.retakeThresholdCount &&
    retakeCount < 3 &&
    !tests.some((t) => t.parentTestId === rootTest.testId && t.status === 'SCHEDULED')

  const showRetakeColumn =
    selectedTest?.status === 'COMPLETED' &&
    selectedTest?.questionCount &&
    selectedTest?.retakeThresholdCount

  const loadTests = useCallback(async () => {
    const list = await fetchTests(classId)
    setTests(list)
    if (list.length > 0 && !selectedId) {
      setSelectedId(String(list[0].testId))
    }
  }, [classId, selectedId])

  const loadAnswerKeys = useCallback(async () => {
    if (!selectedId) {
      setQuestionCount(0)
      setAnswerKeyDraft({ questionCount: 0, answers: [] })
      setSavedAnswerKey({ questionCount: 0, answers: [] })
      return
    }
    const data = await fetchTestAnswerKeys(classId, selectedId)
    const count = data.questionCount || selectedTest?.questionCount || 0
    const answers = answersFromKeyResponse(data)
    setQuestionCount(count)
    setAnswerKeyDraft({ questionCount: count, answers: [...answers] })
    setSavedAnswerKey({ questionCount: count, answers: [...answers] })
  }, [classId, selectedId, selectedTest?.questionCount])

  const loadScores = useCallback(async () => {
    if (!selectedId) {
      setScores([])
      setStudentAnswerDraft({})
      return
    }
    const rows = await fetchTestScores(classId, selectedId)
    setScores(rows)
    const count = questionCount || selectedTest?.questionCount || 0
    const draft = {}
    for (const row of rows) {
      draft[row.studentId] = answersFromRow(row, count)
    }
    setStudentAnswerDraft(draft)
  }, [classId, selectedId, questionCount, selectedTest?.questionCount])

  useEffect(() => {
    ;(async () => {
      setLoading(true)
      onError('')
      try {
        await loadTests()
      } catch (err) {
        onError(err.message)
      } finally {
        setLoading(false)
      }
    })()
  }, [loadTests, onError])

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
    loadScores().catch((err) => onError(err.message))
  }, [loadScores, onError])

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
      const saved = await saveTestAnswerKeys(classId, selectedId, {
        questionCount,
        answers: answerKeyDraft.answers,
      })
      const answers = answersFromKeyResponse(saved)
      setQuestionCount(saved.questionCount)
      setAnswerKeyDraft({ questionCount: saved.questionCount, answers: [...answers] })
      setSavedAnswerKey({ questionCount: saved.questionCount, answers: [...answers] })
      await loadTests()
      await loadScores()
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
      await gradeTestScore(classId, selectedId, studentId, {
        answers: gradeModalDraft,
      })
      await loadTests()
      await loadScores()
      closeGradeModal()
    } catch (err) {
      onError(err.message)
    } finally {
      setGrading(false)
    }
  }

  async function handleCompleteTest() {
    setSubmitting(true)
    onError('')
    try {
      await completeTest(classId, selectedId)
      await loadTests()
      await loadScores()
    } catch (err) {
      onError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  async function handleCreate(e) {
    e.preventDefault()
    if (!form.title.trim() || !form.testDate) return
    setSubmitting(true)
    onError('')
    try {
      const created = await createTest(classId, {
        title: form.title.trim(),
        testAt: toInstant(form.testDate, form.testTime),
        questionCount: form.questionCount ? Number(form.questionCount) : null,
        retakeThresholdCount: form.retakeThresholdCount
          ? Number(form.retakeThresholdCount)
          : null,
      })
      setForm({
        title: '',
        testDate: '',
        testTime: '14:00',
        questionCount: '',
        retakeThresholdCount: '',
      })
      await loadTests()
      setSelectedId(String(created.testId))
    } catch (err) {
      onError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  async function handleCreateRetake(e) {
    e.preventDefault()
    if (!retakeForm.testDate) return
    setSubmitting(true)
    onError('')
    try {
      const created = await createTestRetake(classId, selectedId, {
        testAt: toInstant(retakeForm.testDate, retakeForm.testTime),
      })
      setRetakeOpen(false)
      setRetakeForm({ testDate: '', testTime: '14:00' })
      await loadTests()
      setSelectedId(String(created.testId))
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
      <h3 className="ams-class-detail__heading">{verifyOnly ? '테스트 확인' : '테스트'}</h3>
      <p className="ams-class-detail__hint-inline">
        {verifyOnly
          ? '정답지를 설정한 뒤 학생별 「답안 입력」에서 답을 입력하고 채점하세요. 시험 완료 시 석차가 계산됩니다.'
          : '문항 수·합격 기준을 설정하면 재시험 대상이 자동 판정됩니다.'}
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
            시험일
            <input
              type="date"
              value={form.testDate}
              onChange={(e) => setForm({ ...form, testDate: e.target.value })}
              required
            />
          </label>
          <label>
            시각
            <input
              type="time"
              value={form.testTime}
              onChange={(e) => setForm({ ...form, testTime: e.target.value })}
            />
          </label>
          <label>
            문항 수
            <input
              type="number"
              min={1}
              value={form.questionCount}
              onChange={(e) => setForm({ ...form, questionCount: e.target.value })}
            />
          </label>
          <label>
            합격 기준 (맞은 문항 수)
            <input
              type="number"
              min={1}
              value={form.retakeThresholdCount}
              onChange={(e) => setForm({ ...form, retakeThresholdCount: e.target.value })}
            />
          </label>
          <button type="submit" className="ams-btn ams-btn--primary" disabled={submitting}>
            테스트 등록
          </button>
        </form>
      )}

      {tests.length === 0 ? (
        <p className="ams-class-detail__empty">등록된 테스트가 없습니다.</p>
      ) : (
        <>
          <label className="ams-assignment-form__full">
            테스트 선택
            <select value={selectedId} onChange={(e) => setSelectedId(e.target.value)}>
              {tests.map((t) => (
                <option key={t.testId} value={t.testId}>
                  [{STATUS_LABEL[t.status] || t.status}] {t.title} —{' '}
                  {new Date(t.testAt).toLocaleString('ko-KR')}
                  {t.retakeAttemptNo > 0 ? ` · 재시험 ${t.retakeAttemptNo}` : ''}
                  {t.classAverage != null ? ` · 반평균 ${t.classAverage}` : ''}
                </option>
              ))}
            </select>
          </label>

          {selectedTest && (
            <p className="ams-class-detail__meta">
              {selectedTest.questionCount ? `${selectedTest.questionCount}문항` : '문항 수 미설정'}
              {selectedTest.retakeThresholdCount
                ? ` · 합격 ${selectedTest.retakeThresholdCount}문항 이상`
                : ''}
            </p>
          )}

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
                  {selectedTest?.status !== 'COMPLETED' && (
                    <button
                      type="button"
                      className="ams-btn ams-btn--ghost"
                      disabled={submitting}
                      onClick={handleCompleteTest}
                    >
                      시험 완료 (석차 계산)
                    </button>
                  )}
                </>
              )}
              {canScheduleRetake && (
                <button
                  type="button"
                  className="ams-btn ams-btn--ghost"
                  disabled={submitting}
                  onClick={() => setRetakeOpen(true)}
                >
                  재시험 등록 ({retakeCount}/3)
                </button>
              )}
            </div>
          )}

          {scores.length > 0 && (
            <div className="ams-submission-table-wrap">
              <table className="ams-submission-table ams-submission-table--homework">
                <thead>
                  <tr>
                    <th>학생</th>
                    <th>맞은 수</th>
                    <th>점수</th>
                    <th>석차</th>
                    {showRetakeColumn && <th>재시험</th>}
                    <th>상태</th>
                    <th>{canManage ? '답안' : '확인'}</th>
                  </tr>
                </thead>
                <tbody>
                  {scores.map((s) => (
                    <tr key={s.studentId}>
                      <td>{s.studentName}</td>
                      <td>
                        {s.correctCount != null ? `${s.correctCount}/${questionCount || '?'}` : '—'}
                      </td>
                      <td>{s.rawScore != null ? `${s.rawScore}점` : '—'}</td>
                      <td>{s.rank != null ? `${s.rank}등` : '—'}</td>
                      {showRetakeColumn && (
                        <td>
                          <span
                            className={
                              s.needsRetake === true
                                ? 'ams-test-retake-badge ams-test-retake-badge--fail'
                                : s.needsRetake === false
                                  ? 'ams-test-retake-badge ams-test-retake-badge--pass'
                                  : ''
                            }
                          >
                            {retakeStatusLabel(s)}
                          </span>
                        </td>
                      )}
                      <td>{s.gradedAt ? '채점됨' : hasAnswerKey ? '미채점' : '—'}</td>
                      <td>
                        {(canManage && hasAnswerKey) || (!canManage && s.gradedAt) ? (
                          <button
                            type="button"
                            className="ams-btn ams-btn--ghost ams-homework-row-btn"
                            onClick={() => openGradeModal(s.studentId)}
                          >
                            {canManage ? (s.gradedAt ? '수정' : '답안 입력') : '보기'}
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
          testTitle={selectedTest?.title ?? ''}
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
          testTitle={selectedTest?.title ?? ''}
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

      {retakeOpen && (
        <TestModalBackdrop label="재시험 등록" onClose={() => setRetakeOpen(false)}>
          <header className="ams-homework-modal__header">
            <h4 className="ams-homework-modal__title">재시험 등록</h4>
            <button
              type="button"
              className="ams-homework-modal__close"
              onClick={() => setRetakeOpen(false)}
              aria-label="닫기"
            >
              ×
            </button>
          </header>
          <form className="ams-homework-modal__body" onSubmit={handleCreateRetake}>
            <label className="ams-homework-modal__count">
              시험일
              <input
                type="date"
                value={retakeForm.testDate}
                required
                onChange={(e) => setRetakeForm({ ...retakeForm, testDate: e.target.value })}
              />
            </label>
            <label className="ams-homework-modal__count">
              시각
              <input
                type="time"
                value={retakeForm.testTime}
                onChange={(e) => setRetakeForm({ ...retakeForm, testTime: e.target.value })}
              />
            </label>
            <footer className="ams-homework-modal__footer">
              <button type="button" className="ams-btn ams-btn--ghost" onClick={() => setRetakeOpen(false)}>
                취소
              </button>
              <button type="submit" className="ams-btn ams-btn--primary" disabled={submitting}>
                {submitting ? '등록 중…' : '재시험 등록'}
              </button>
            </footer>
          </form>
        </TestModalBackdrop>
      )}
    </section>
  )
}
