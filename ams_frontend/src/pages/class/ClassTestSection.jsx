import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  AnswerKeyFileField,
  AnswerKeyUploadModal,
  openAnswerKeyPdf,
  SubmissionResultModal,
} from '../../components/AssignmentGradingModals'
import {
  completeTest,
  createTest,
  createTestRetake,
  fetchTestAnswerKeys,
  fetchTestScores,
  fetchTests,
  gradeTestScore,
  testAnswerKeyPdfPath,
  toInstant,
  uploadTestAnswerKey,
} from '../../api/classesApi'
import StudentTargetPicker from '../../components/StudentTargetPicker'
import {
  buildTargetStudentIdsPayload,
  createInitialTarget,
  formatTargetSummary,
} from '../../utils/assignmentTargets'

const STATUS_LABEL = { SCHEDULED: '예정', COMPLETED: '완료' }

function TestModalBackdrop({ label, onClose, children }) {
  return (
    <div className="ams-homework-modal-backdrop" onClick={onClose}>
      <div className="ams-homework-modal" role="dialog" aria-modal="true" aria-label={label} onClick={(e) => e.stopPropagation()}>
        {children}
      </div>
    </div>
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
  const [hasAnswerKeyFile, setHasAnswerKeyFile] = useState(false)
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [saving, setSaving] = useState(false)
  const [modal, setModal] = useState(null)
  const [retakeOpen, setRetakeOpen] = useState(false)
  const [retakeForm, setRetakeForm] = useState({ testDate: '', testTime: '14:00' })
  const [form, setForm] = useState({
    title: '',
    testDate: '',
    testTime: '14:00',
    questionCount: '',
    retakeThresholdCount: '',
  })
  const [createAnswerKeyFile, setCreateAnswerKeyFile] = useState(null)
  const [createTarget, setCreateTarget] = useState(() => createInitialTarget(true))

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

  const isRetake = Boolean(selectedTest?.parentTestId)

  const resultStudent = useMemo(() => {
    if (modal?.type !== 'result') return null
    return scores.find((s) => s.studentId === modal.studentId) ?? null
  }, [modal, scores])

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

  const loadAnswerKeyMeta = useCallback(async () => {
    if (!selectedId) {
      setQuestionCount(0)
      setHasAnswerKeyFile(false)
      return
    }
    const data = await fetchTestAnswerKeys(classId, selectedId)
    setQuestionCount(data.questionCount || selectedTest?.questionCount || 0)
    setHasAnswerKeyFile(Boolean(data.hasAnswerKeyFile))
  }, [classId, selectedId, selectedTest?.questionCount])

  const loadScores = useCallback(async () => {
    if (!selectedId) {
      setScores([])
      return
    }
    const rows = await fetchTestScores(classId, selectedId)
    setScores(rows)
  }, [classId, selectedId])

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
    loadAnswerKeyMeta().catch((err) => onError(err.message))
  }, [loadAnswerKeyMeta, onError])

  useEffect(() => {
    loadScores().catch((err) => onError(err.message))
  }, [loadScores, onError])

  useEffect(() => {
    setModal(null)
  }, [selectedId])

  async function handleUploadAnswerKey(file, count) {
    const uploadTestId = isRetake ? rootTest?.testId : selectedId
    if (!uploadTestId) return
    setSubmitting(true)
    onError('')
    try {
      await uploadTestAnswerKey(classId, uploadTestId, file, count)
      await loadAnswerKeyMeta()
      await loadTests()
      setModal(null)
    } catch (err) {
      onError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  async function handleViewAnswerKeyPdf() {
    onError('')
    try {
      await openAnswerKeyPdf(testAnswerKeyPdfPath(classId, selectedId))
    } catch (err) {
      onError(err.message)
    }
  }

  async function handleSaveResult(wrongQuestionNos) {
    if (!resultStudent) return
    setSaving(true)
    onError('')
    try {
      await gradeTestScore(classId, selectedId, resultStudent.studentId, {
        wrongQuestionNos,
      })
      await loadTests()
      await loadScores()
      setModal(null)
    } catch (err) {
      onError(err.message)
    } finally {
      setSaving(false)
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
    if (createTarget.mode === 'custom' && createTarget.studentIds.length === 0) {
      onError('대상 학생을 한 명 이상 선택하세요.')
      return
    }
    setSubmitting(true)
    onError('')
    try {
      const targetStudentIds = buildTargetStudentIdsPayload(createTarget, true)
      const payload = {
        title: form.title.trim(),
        testAt: toInstant(form.testDate, form.testTime),
        questionCount: form.questionCount ? Number(form.questionCount) : null,
        retakeThresholdCount: form.retakeThresholdCount
          ? Number(form.retakeThresholdCount)
          : null,
      }
      if (targetStudentIds !== undefined) {
        payload.targetStudentIds = targetStudentIds
      }
      const created = await createTest(classId, payload)
      if (createAnswerKeyFile) {
        const count = Number(form.questionCount)
        if (!count) {
          throw new Error('정답지 업로드 시 문항 수를 입력하세요.')
        }
        await uploadTestAnswerKey(classId, created.testId, createAnswerKeyFile, count)
      }
      setForm({
        title: '',
        testDate: '',
        testTime: '14:00',
        questionCount: '',
        retakeThresholdCount: '',
      })
      setCreateAnswerKeyFile(null)
      setCreateTarget(createInitialTarget(true))
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

  if (loading) {
    return <p className="ams-class-detail__empty">불러오는 중…</p>
  }

  return (
    <section className="ams-class-detail__section">
      <h3 className="ams-class-detail__heading">{verifyOnly ? '테스트 확인' : '테스트'}</h3>
      <p className="ams-class-detail__hint-inline">
        {verifyOnly
          ? '정답지 PDF를 업로드한 뒤 학생별 맞은 문항 수·틀린 문항을 입력하세요. 시험 완료 시 석차가 계산됩니다.'
          : '정답지 PDF 업로드 후 종이 채점 결과를 입력합니다.'}
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
          <StudentTargetPicker
            className="ams-assignment-form__full"
            classId={classId}
            allByDefault
            value={createTarget}
            onChange={setCreateTarget}
            disabled={submitting}
          />
          <AnswerKeyFileField
            file={createAnswerKeyFile}
            disabled={submitting}
            onFileChange={setCreateAnswerKeyFile}
          />
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
              {selectedTest.targets ? ` · 대상 ${formatTargetSummary(selectedTest.targets)}` : ''}
            </p>
          )}

          {selectedId && (
            <div className="ams-homework-toolbar">
              {canManage && (
                <>
                  {!isRetake && (
                    <button
                      type="button"
                      className="ams-btn ams-btn--primary"
                      onClick={() => setModal({ type: 'answer-key' })}
                    >
                      정답지 {hasAnswerKeyFile ? '관리' : '업로드'}
                      {hasAnswerKeyFile && (
                        <span className="ams-homework-toolbar__badge">등록됨</span>
                      )}
                    </button>
                  )}
                  {hasAnswerKeyFile && (
                    <button type="button" className="ams-btn ams-btn--ghost" onClick={handleViewAnswerKeyPdf}>
                      정답지 보기
                    </button>
                  )}
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
                    <th>{canManage ? '결과' : '확인'}</th>
                  </tr>
                </thead>
                <tbody>
                  {scores.map((s) => (
                    <tr key={s.studentId}>
                      <td>{s.studentName}</td>
                      <td>
                        {s.correctCount != null ? `${s.correctCount}/${questionCount || '?'}` : '—'}
                      </td>
                      <td>{s.rawScore != null ? `${s.rawScore}%` : '—'}</td>
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
                      <td>{s.gradedAt ? '입력됨' : hasAnswerKeyFile ? '미입력' : '—'}</td>
                      <td>
                        {(canManage && hasAnswerKeyFile) || (!canManage && s.gradedAt) ? (
                          <button
                            type="button"
                            className="ams-btn ams-btn--ghost ams-homework-row-btn"
                            onClick={() => setModal({ type: 'result', studentId: s.studentId })}
                          >
                            {canManage ? (s.gradedAt ? '수정' : '결과 입력') : '보기'}
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

      {modal?.type === 'answer-key' && canManage && !isRetake && (
        <AnswerKeyUploadModal
          title={selectedTest?.title ?? ''}
          questionCount={questionCount}
          hasAnswerKeyFile={hasAnswerKeyFile}
          submitting={submitting}
          onQuestionCountChange={(value) => setQuestionCount(Math.max(0, Number(value) || 0))}
          onUpload={handleUploadAnswerKey}
          onViewPdf={handleViewAnswerKeyPdf}
          onClose={() => setModal(null)}
        />
      )}

      {modal?.type === 'result' && resultStudent && questionCount > 0 && (
        <SubmissionResultModal
          key={`test-result-${resultStudent.studentId}`}
          studentName={resultStudent.studentName}
          assignmentTitle={selectedTest?.title ?? ''}
          questionCount={questionCount}
          savedRow={{
            ...resultStudent,
            score: resultStudent.rawScore,
            completedAt: resultStudent.gradedAt,
          }}
          canManage={canManage && hasAnswerKeyFile}
          saving={saving}
          onSave={handleSaveResult}
          onClose={() => setModal(null)}
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
