import { useCallback, useEffect, useMemo, useState } from 'react'
import { useParams } from 'react-router-dom'
import {
  AssignmentDetailPanel,
  AssignmentStudentCard,
} from '../../components/AssignmentVerifyBoard'
import AssignmentDetailPageShell from '../../components/AssignmentDetailPageShell'
import {
  AnswerKeyUploadModal,
  CorrectCountResultModal,
  openAnswerKeyPdf,
} from '../../components/AssignmentGradingModals'
import {
  fetchClassDetail,
  fetchHomeworkAnswerKeys,
  fetchHomeworkSubmissions,
  fetchHomeworks,
  gradeHomeworkSubmission,
  homeworkAnswerKeyPdfPath,
  uploadHomeworkAnswerKey,
} from '../../api/classesApi'
import { ASSIGNMENT_STATUS_LABEL } from '../../utils/assignmentVerify'
import { formatTargetSummary } from '../../utils/assignmentTargets'

export default function ClassHomeworkDetailPage() {
  const { classId, homeworkId } = useParams()
  const [classDetail, setClassDetail] = useState(null)
  const [homework, setHomework] = useState(null)
  const [submissions, setSubmissions] = useState([])
  const [questionCount, setQuestionCount] = useState(0)
  const [hasAnswerKeyFile, setHasAnswerKeyFile] = useState(false)
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [modal, setModal] = useState(null)

  const canManage = classDetail?.canEditContent ?? false

  const resultStudent = useMemo(() => {
    if (modal?.type !== 'result') return null
    return submissions.find((s) => s.studentId === modal.studentId) ?? null
  }, [modal, submissions])

  const gradedSummary = useMemo(() => {
    if (submissions.length === 0) return null
    const done = submissions.filter((s) => s.score != null).length
    return `${done}/${submissions.length}명 결과 입력`
  }, [submissions])

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const [detail, list, answerMeta, rows] = await Promise.all([
        fetchClassDetail(classId),
        fetchHomeworks(classId),
        fetchHomeworkAnswerKeys(classId, homeworkId),
        fetchHomeworkSubmissions(classId, homeworkId),
      ])
      const found = list.find((h) => String(h.homeworkId) === homeworkId)
      if (!found) {
        setError('숙제를 찾을 수 없습니다.')
        setHomework(null)
        return
      }
      setClassDetail(detail)
      setHomework(found)
      setQuestionCount(answerMeta.questionCount || found.questionCount || 0)
      setHasAnswerKeyFile(Boolean(answerMeta.hasAnswerKeyFile))
      setSubmissions(rows)
    } catch (err) {
      setError(err.message)
      setHomework(null)
    } finally {
      setLoading(false)
    }
  }, [classId, homeworkId])

  useEffect(() => {
    load()
  }, [load])

  async function handleUploadAnswerKey(file, count) {
    setSubmitting(true)
    setError('')
    try {
      await uploadHomeworkAnswerKey(classId, homeworkId, file, count)
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
      await openAnswerKeyPdf(homeworkAnswerKeyPdfPath(classId, homeworkId))
    } catch (err) {
      setError(err.message)
    }
  }

  async function handleSaveResult(correctCount) {
    if (!resultStudent) return
    setSaving(true)
    setError('')
    try {
      const updated = await gradeHomeworkSubmission(classId, homeworkId, resultStudent.studentId, {
        correctCount,
      })
      await load()
      setModal(null)
    } catch (err) {
      setError(err.message)
    } finally {
      setSaving(false)
    }
  }

  if (loading) {
    return (
      <AssignmentDetailPageShell
        classId={classId}
        listTab="homework"
        listLabel="숙제 확인"
        error={error}
      >
        <p className="ams-class-detail__empty">불러오는 중…</p>
      </AssignmentDetailPageShell>
    )
  }

  if (!homework) {
    return (
      <AssignmentDetailPageShell
        classId={classId}
        className={classDetail?.name}
        classSubject={classDetail?.subject}
        classRoom={classDetail?.classroom}
        listTab="homework"
        listLabel="숙제 확인"
        error={error || '숙제를 찾을 수 없습니다.'}
      />
    )
  }

  return (
    <AssignmentDetailPageShell
      classId={classId}
      className={classDetail?.name}
      classSubject={classDetail?.subject}
      classRoom={classDetail?.classroom}
      listTab="homework"
      listLabel="숙제 확인"
      title={homework.title}
      error={error}
    >
      <AssignmentDetailPanel
        meta={
          <>
            <p>
              <span
                className={`ams-assignment-board__status ams-assignment-board__status--${homework.status === 'COMPLETED' ? 'completed' : 'scheduled'}`}
              >
                {ASSIGNMENT_STATUS_LABEL[homework.status] || homework.status}
              </span>
              {homework.questionCount ? ` · ${homework.questionCount}문항` : ' · 문항 수 미설정'}
              {homework.targets ? ` · 대상 ${formatTargetSummary(homework.targets)}` : ''}
              {hasAnswerKeyFile ? ' · 정답지 등록됨(참고)' : ''}
              {canManage && gradedSummary ? ` · ${gradedSummary}` : ''}
            </p>
            {!canManage && questionCount <= 0 && (
              <p>문항 수가 설정되지 않았습니다.</p>
            )}
          </>
        }
        toolbar={
          canManage ? (
            <>
              <button
                type="button"
                className="ams-btn ams-btn--primary ams-btn--sm"
                onClick={() => setModal({ type: 'answer-key' })}
              >
                정답지 {hasAnswerKeyFile ? '관리' : '업로드'}
                {hasAnswerKeyFile && <span className="ams-homework-toolbar__badge">등록됨</span>}
              </button>
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
          ) : null
        }
        students={submissions.map((s) => ({ key: s.studentId, row: s }))}
        renderStudentCard={({ row: s }) => {
          const canOpen = (canManage && questionCount > 0) || (!canManage && s.score != null)
          const stats = []
          if (s.correctCount != null) stats.push(`맞은 수 ${s.correctCount}/${questionCount || '?'}`)
          if (s.score != null) stats.push(`점수 ${s.score}%`)
          return (
            <AssignmentStudentCard
              name={s.studentName}
              stats={stats}
              statusLabel={s.score != null ? '입력 완료' : questionCount > 0 ? '미입력' : null}
              statusTone={s.score != null ? 'done' : questionCount > 0 ? 'pending' : 'muted'}
              action={
                canOpen ? (
                  <button
                    type="button"
                    className="ams-btn ams-btn--ghost ams-btn--sm"
                    onClick={() => setModal({ type: 'result', studentId: s.studentId })}
                  >
                    {canManage ? (s.score != null ? '수정' : '결과 입력') : '보기'}
                  </button>
                ) : null
              }
            />
          )
        }}
      />

      {modal?.type === 'answer-key' && canManage && (
        <AnswerKeyUploadModal
          title={homework.title}
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
        <CorrectCountResultModal
          key={`hw-result-${resultStudent.studentId}`}
          studentName={resultStudent.studentName}
          assignmentTitle={homework.title}
          questionCount={questionCount}
          savedRow={resultStudent}
          canManage={canManage}
          saving={saving}
          onSave={handleSaveResult}
          onClose={() => setModal(null)}
        />
      )}
    </AssignmentDetailPageShell>
  )
}
