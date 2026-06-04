import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import {
  AssignmentDetailPanel,
  AssignmentStudentCard,
} from '../../components/AssignmentVerifyBoard'
import AssignmentDetailPageShell from '../../components/AssignmentDetailPageShell'
import {
  AnswerKeyUploadModal,
  CorrectCountResultModal,
  openAnswerKeyPdf,
  SubmissionResultModal,
} from '../../components/AssignmentGradingModals'
import {
  createTestRetake,
  fetchClassDetail,
  fetchTestAnswerKeys,
  fetchTestScores,
  fetchTests,
  gradeTestScore,
  testAnswerKeyPdfPath,
  toInstant,
  uploadTestAnswerKey,
} from '../../api/classesApi'
import { ASSIGNMENT_STATUS_LABEL } from '../../utils/assignmentVerify'
import { formatTargetSummary } from '../../utils/assignmentTargets'

function TestModalBackdrop({ label, onClose, children }) {
  return (
    <div className="ams-homework-modal-backdrop" onClick={onClose}>
      <div
        className="ams-homework-modal"
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

function retakeStatusLabel(row) {
  if (row.needsRetake == null) return '—'
  return row.needsRetake ? '재시험 대상' : '합격'
}

export default function ClassTestDetailPage() {
  const { classId, testId } = useParams()
  const navigate = useNavigate()
  const [classDetail, setClassDetail] = useState(null)
  const [tests, setTests] = useState([])
  const [test, setTest] = useState(null)
  const [scores, setScores] = useState([])
  const [questionCount, setQuestionCount] = useState(0)
  const [hasAnswerKeyFile, setHasAnswerKeyFile] = useState(false)
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [modal, setModal] = useState(null)
  const [retakeOpen, setRetakeOpen] = useState(false)
  const [retakeForm, setRetakeForm] = useState({ testDate: '', testTime: '14:00' })

  const canManage = classDetail?.canEditContent ?? false

  const rootTest = test ? tests.find((t) => t.testId === test.rootTestId) ?? test : null
  const retakeCount = rootTest
    ? tests.filter((t) => t.parentTestId === rootTest.testId).length
    : 0
  const isRetake = Boolean(test?.parentTestId)
  const countOnlyGrading = test?.countOnlyGrading ?? false

  const resultStudent = useMemo(() => {
    if (modal?.type !== 'result') return null
    return scores.find((s) => s.studentId === modal.studentId) ?? null
  }, [modal, scores])

  const canScheduleRetake =
    canManage &&
    test?.status === 'COMPLETED' &&
    rootTest?.questionCount &&
    rootTest?.retakeThresholdCount &&
    retakeCount < 3 &&
    !tests.some((t) => t.parentTestId === rootTest.testId && t.status === 'SCHEDULED')

  const showRetakeColumn =
    test?.status === 'COMPLETED' && test?.questionCount && test?.retakeThresholdCount

  const gradedSummary = useMemo(() => {
    if (scores.length === 0) return null
    const done = scores.filter((s) => s.rawScore != null).length
    return `${done}/${scores.length}명 결과 입력`
  }, [scores])

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const [detail, list, answerMeta, rows] = await Promise.all([
        fetchClassDetail(classId),
        fetchTests(classId),
        fetchTestAnswerKeys(classId, testId),
        fetchTestScores(classId, testId),
      ])
      const found = list.find((t) => String(t.testId) === testId)
      if (!found) {
        setError('테스트를 찾을 수 없습니다.')
        setTest(null)
        return
      }
      setClassDetail(detail)
      setTests(list)
      setTest(found)
      setQuestionCount(answerMeta.questionCount || found.questionCount || 0)
      setHasAnswerKeyFile(Boolean(answerMeta.hasAnswerKeyFile))
      setScores(rows)
    } catch (err) {
      setError(err.message)
      setTest(null)
    } finally {
      setLoading(false)
    }
  }, [classId, testId])

  useEffect(() => {
    load()
  }, [load])

  async function handleUploadAnswerKey(file, count) {
    const uploadTestId = isRetake ? rootTest?.testId : testId
    if (!uploadTestId) return
    setSubmitting(true)
    setError('')
    try {
      await uploadTestAnswerKey(classId, uploadTestId, file, count)
      await load()
      setModal(null)
    } catch (err) {
      setError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  async function handleViewAnswerKeyPdf() {
    setError('')
    try {
      await openAnswerKeyPdf(testAnswerKeyPdfPath(classId, testId))
    } catch (err) {
      setError(err.message)
    }
  }

  async function handleSaveResult(value) {
    if (!resultStudent) return
    setSaving(true)
    setError('')
    try {
      const payload = countOnlyGrading ? { correctCount: value } : { wrongQuestionNos: value }
      await gradeTestScore(classId, testId, resultStudent.studentId, payload)
      await load()
      setModal(null)
    } catch (err) {
      setError(err.message)
    } finally {
      setSaving(false)
    }
  }

  async function handleCreateRetake(e) {
    e.preventDefault()
    if (!retakeForm.testDate) return
    setSubmitting(true)
    setError('')
    try {
      const created = await createTestRetake(classId, testId, {
        testAt: toInstant(retakeForm.testDate, retakeForm.testTime),
      })
      setRetakeOpen(false)
      setRetakeForm({ testDate: '', testTime: '14:00' })
      navigate(`/classes/${classId}/tests/${created.testId}`)
    } catch (err) {
      setError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) {
    return (
      <AssignmentDetailPageShell classId={classId} listTab="test" listLabel="테스트 확인" error={error}>
        <p className="ams-class-detail__empty">불러오는 중…</p>
      </AssignmentDetailPageShell>
    )
  }

  if (!test) {
    return (
      <AssignmentDetailPageShell
        classId={classId}
        className={classDetail?.name}
        classSubject={classDetail?.subject}
        classRoom={classDetail?.classroom}
        listTab="test"
        listLabel="테스트 확인"
        error={error || '테스트를 찾을 수 없습니다.'}
      />
    )
  }

  return (
    <AssignmentDetailPageShell
      classId={classId}
      className={classDetail?.name}
      classSubject={classDetail?.subject}
      classRoom={classDetail?.classroom}
      listTab="test"
      listLabel="테스트 확인"
      title={test.title}
      error={error}
    >
      <AssignmentDetailPanel
        meta={
          <p>
            <span
              className={`ams-assignment-board__status ams-assignment-board__status--${test.status === 'COMPLETED' ? 'completed' : 'scheduled'}`}
            >
              {ASSIGNMENT_STATUS_LABEL[test.status] || test.status}
            </span>
            {` · ${new Date(test.testAt).toLocaleString('ko-KR')}`}
            {test.retakeAttemptNo > 0 ? ` · 재시험 ${test.retakeAttemptNo}회` : ''}
            {test.questionCount ? ` · ${test.questionCount}문항` : ' · 문항 수 미설정'}
            {test.retakeThresholdCount ? ` · 합격 ${test.retakeThresholdCount}문항 이상` : ''}
            {test.targets ? ` · 대상 ${formatTargetSummary(test.targets)}` : ''}
            {countOnlyGrading
              ? hasAnswerKeyFile
                ? ' · 정답지 등록됨(참고)'
                : ''
              : hasAnswerKeyFile
                ? ' · 정답지 등록됨(참고)'
                : ''}
            {gradedSummary ? ` · ${gradedSummary}` : ''}
            {test.classAverage != null ? ` · 반평균 ${test.classAverage}%` : ''}
          </p>
        }
        toolbar={
          <>
            {canManage && (
              <>
                {!isRetake && (
                  <button
                    type="button"
                    className="ams-btn ams-btn--primary ams-btn--sm"
                    onClick={() => setModal({ type: 'answer-key' })}
                  >
                    정답지 {hasAnswerKeyFile ? '관리' : '업로드'}
                    {hasAnswerKeyFile && <span className="ams-homework-toolbar__badge">등록됨</span>}
                  </button>
                )}
                {hasAnswerKeyFile && (
                  <button
                    type="button"
                    className="ams-btn ams-btn--ghost ams-btn--sm"
                    onClick={handleViewAnswerKeyPdf}
                  >
                    정답지 보기
                  </button>
                )}
              </>
            )}
            {canScheduleRetake && (
              <button
                type="button"
                className="ams-btn ams-btn--ghost ams-btn--sm"
                disabled={submitting}
                onClick={() => setRetakeOpen(true)}
              >
                재시험 ({retakeCount}/3)
              </button>
            )}
          </>
        }
        students={scores.map((s) => ({ key: s.studentId, row: s }))}
        renderStudentCard={({ row: s }) => {
          const canGrade = questionCount > 0
          const canOpen = (canManage && canGrade) || (!canManage && s.rawScore != null)
          const stats = []
          if (s.correctCount != null) stats.push(`맞은 수 ${s.correctCount}/${questionCount || '?'}`)
          if (s.rawScore != null) stats.push(`점수 ${s.rawScore}%`)
          if (s.rank != null) stats.push(`${s.rank}등`)
          if (showRetakeColumn && s.needsRetake != null) stats.push(retakeStatusLabel(s))
          return (
            <AssignmentStudentCard
              name={s.studentName}
              stats={stats}
              statusLabel={s.rawScore != null ? '입력 완료' : canGrade ? '미입력' : null}
              statusTone={s.rawScore != null ? 'done' : canGrade ? 'pending' : 'muted'}
              action={
                canOpen ? (
                  <button
                    type="button"
                    className="ams-btn ams-btn--ghost ams-btn--sm"
                    onClick={() => setModal({ type: 'result', studentId: s.studentId })}
                  >
                    {canManage ? (s.rawScore != null ? '수정' : '결과 입력') : '보기'}
                  </button>
                ) : null
              }
            />
          )
        }}
      />

      {modal?.type === 'answer-key' && canManage && !isRetake && (
        <AnswerKeyUploadModal
          title={test.title}
          questionCount={questionCount}
          hasAnswerKeyFile={hasAnswerKeyFile}
          submitting={submitting}
          onQuestionCountChange={(value) => setQuestionCount(Math.max(0, Number(value) || 0))}
          onUpload={handleUploadAnswerKey}
          onViewPdf={handleViewAnswerKeyPdf}
          onClose={() => setModal(null)}
        />
      )}

      {modal?.type === 'result' && resultStudent && questionCount > 0 && countOnlyGrading && (
        <CorrectCountResultModal
          key={`test-result-${resultStudent.studentId}`}
          studentName={resultStudent.studentName}
          assignmentTitle={test.title}
          questionCount={questionCount}
          savedRow={{
            ...resultStudent,
            score: resultStudent.rawScore,
          }}
          canManage={canManage}
          saving={saving}
          onSave={handleSaveResult}
          onClose={() => setModal(null)}
        />
      )}

      {modal?.type === 'result' && resultStudent && questionCount > 0 && !countOnlyGrading && (
        <SubmissionResultModal
          key={`test-result-${resultStudent.studentId}`}
          studentName={resultStudent.studentName}
          assignmentTitle={test.title}
          questionCount={questionCount}
          savedRow={{
            ...resultStudent,
            score: resultStudent.rawScore,
            completedAt: resultStudent.gradedAt,
          }}
          canManage={canManage}
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
    </AssignmentDetailPageShell>
  )
}
