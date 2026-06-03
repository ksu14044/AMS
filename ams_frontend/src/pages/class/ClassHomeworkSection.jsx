import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  AnswerKeyFileField,
  AnswerKeyUploadModal,
  openAnswerKeyPdf,
  SubmissionResultModal,
} from '../../components/AssignmentGradingModals'
import {
  completeHomework,
  createHomework,
  fetchHomeworkAnswerKeys,
  fetchHomeworkSubmissions,
  fetchHomeworks,
  gradeHomeworkSubmission,
  homeworkAnswerKeyPdfPath,
  uploadHomeworkAnswerKey,
} from '../../api/classesApi'
import StudentTargetPicker from '../../components/StudentTargetPicker'
import {
  buildTargetStudentIdsPayload,
  createInitialTarget,
  formatTargetSummary,
} from '../../utils/assignmentTargets'

const STATUS_LABEL = { SCHEDULED: '예정', COMPLETED: '완료' }

export default function ClassHomeworkSection({ classId, canManage, verifyOnly = false, onError }) {
  const [homeworks, setHomeworks] = useState([])
  const [selectedId, setSelectedId] = useState('')
  const [submissions, setSubmissions] = useState([])
  const [questionCount, setQuestionCount] = useState(0)
  const [hasAnswerKeyFile, setHasAnswerKeyFile] = useState(false)
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [saving, setSaving] = useState(false)
  const [modal, setModal] = useState(null)
  const [form, setForm] = useState({ title: '', questionCount: '' })
  const [createAnswerKeyFile, setCreateAnswerKeyFile] = useState(null)
  const [createTarget, setCreateTarget] = useState(() => createInitialTarget(true))

  const selectedHomework = useMemo(
    () => homeworks.find((h) => String(h.homeworkId) === selectedId),
    [homeworks, selectedId],
  )

  const resultStudent = useMemo(() => {
    if (modal?.type !== 'result') return null
    return submissions.find((s) => s.studentId === modal.studentId) ?? null
  }, [modal, submissions])

  const loadHomeworks = useCallback(async () => {
    const list = await fetchHomeworks(classId)
    setHomeworks(list)
    if (list.length > 0 && !selectedId) {
      setSelectedId(String(list[0].homeworkId))
    }
  }, [classId, selectedId])

  const loadAnswerKeyMeta = useCallback(async () => {
    if (!selectedId) {
      setQuestionCount(0)
      setHasAnswerKeyFile(false)
      return
    }
    const data = await fetchHomeworkAnswerKeys(classId, selectedId)
    setQuestionCount(data.questionCount || selectedHomework?.questionCount || 0)
    setHasAnswerKeyFile(Boolean(data.hasAnswerKeyFile))
  }, [classId, selectedId, selectedHomework?.questionCount])

  const loadSubmissions = useCallback(async () => {
    if (!selectedId) {
      setSubmissions([])
      return
    }
    const rows = await fetchHomeworkSubmissions(classId, selectedId)
    setSubmissions(rows)
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
    loadAnswerKeyMeta().catch((err) => onError(err.message))
  }, [loadAnswerKeyMeta, onError])

  useEffect(() => {
    loadSubmissions().catch((err) => onError(err.message))
  }, [loadSubmissions, onError])

  useEffect(() => {
    setModal(null)
  }, [selectedId])

  async function handleUploadAnswerKey(file, count) {
    setSubmitting(true)
    onError('')
    try {
      await uploadHomeworkAnswerKey(classId, selectedId, file, count)
      await loadAnswerKeyMeta()
      await loadHomeworks()
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
      await openAnswerKeyPdf(homeworkAnswerKeyPdfPath(classId, selectedId))
    } catch (err) {
      onError(err.message)
    }
  }

  async function handleSaveResult(wrongQuestionNos) {
    if (!resultStudent) return
    setSaving(true)
    onError('')
    try {
      const updated = await gradeHomeworkSubmission(classId, selectedId, resultStudent.studentId, {
        wrongQuestionNos,
      })
      setSubmissions((prev) => prev.map((s) => (s.studentId === resultStudent.studentId ? updated : s)))
      setModal(null)
    } catch (err) {
      onError(err.message)
    } finally {
      setSaving(false)
    }
  }

  async function handleCreate(e) {
    e.preventDefault()
    if (!form.title.trim()) return
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
        questionCount: form.questionCount ? Number(form.questionCount) : null,
      }
      if (targetStudentIds !== undefined) {
        payload.targetStudentIds = targetStudentIds
      }
      const created = await createHomework(classId, payload)
      if (createAnswerKeyFile) {
        const count = Number(form.questionCount)
        if (!count) {
          throw new Error('정답지 업로드 시 문항 수를 입력하세요.')
        }
        await uploadHomeworkAnswerKey(classId, created.homeworkId, createAnswerKeyFile, count)
      }
      setForm({ title: '', questionCount: '' })
      setCreateAnswerKeyFile(null)
      setCreateTarget(createInitialTarget(true))
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

  if (loading) {
    return <p className="ams-class-detail__empty">불러오는 중…</p>
  }

  return (
    <section className="ams-class-detail__section">
      <h3 className="ams-class-detail__heading">{verifyOnly ? '숙제 확인' : '숙제'}</h3>
      <p className="ams-class-detail__hint-inline">
        {verifyOnly
          ? '정답지 PDF를 업로드한 뒤 학생별 맞은 문항 수·틀린 문항을 입력하세요.'
          : '교직원이 정답지 파일을 업로드하고 종이 채점 결과를 입력합니다.'}
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
              placeholder="정답지 업로드 시 입력"
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
              {selectedHomework?.targets && (
                <p className="ams-class-detail__meta">
                  대상: {formatTargetSummary(selectedHomework.targets)}
                </p>
              )}
              {canManage && (
                <>
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
                  {hasAnswerKeyFile && (
                    <button type="button" className="ams-btn ams-btn--ghost" onClick={handleViewAnswerKeyPdf}>
                      정답지 보기
                    </button>
                  )}
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
              {!canManage && !hasAnswerKeyFile && questionCount <= 0 && (
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
                    <th>{canManage ? '결과' : '확인'}</th>
                  </tr>
                </thead>
                <tbody>
                  {submissions.map((s) => (
                    <tr key={s.studentId}>
                      <td>{s.studentName}</td>
                      <td>
                        {s.correctCount != null ? `${s.correctCount}/${questionCount || '?'}` : '—'}
                      </td>
                      <td>{s.score != null ? `${s.score}%` : '—'}</td>
                      <td>{s.completedAt ? '완료' : hasAnswerKeyFile ? '미입력' : '—'}</td>
                      <td>
                        {(canManage && hasAnswerKeyFile) || (!canManage && s.completedAt) ? (
                          <button
                            type="button"
                            className="ams-btn ams-btn--ghost ams-homework-row-btn"
                            onClick={() => setModal({ type: 'result', studentId: s.studentId })}
                          >
                            {canManage ? (s.completedAt ? '수정' : '결과 입력') : '보기'}
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
        <AnswerKeyUploadModal
          title={selectedHomework?.title ?? ''}
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
          key={`hw-result-${resultStudent.studentId}`}
          studentName={resultStudent.studentName}
          assignmentTitle={selectedHomework?.title ?? ''}
          questionCount={questionCount}
          savedRow={resultStudent}
          canManage={canManage && hasAnswerKeyFile}
          saving={saving}
          onSave={handleSaveResult}
          onClose={() => setModal(null)}
        />
      )}
    </section>
  )
}
