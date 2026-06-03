import { useCallback, useEffect, useMemo, useState } from 'react'
import { useParams } from 'react-router-dom'
import { AssignmentVerifyList } from '../../components/AssignmentVerifyBoard'
import { AnswerKeyFileField } from '../../components/AssignmentGradingModals'
import { createHomework, fetchHomeworks, uploadHomeworkAnswerKey } from '../../api/classesApi'
import StudentTargetPicker from '../../components/StudentTargetPicker'
import {
  buildTargetStudentIdsPayload,
  createInitialTarget,
} from '../../utils/assignmentTargets'
import { toHomeworkBoardItems } from '../../utils/assignmentVerify'

export default function ClassHomeworkSection({ classId, canManage, verifyOnly = false, onError }) {
  const { homeworkId: activeHomeworkId } = useParams()
  const [homeworks, setHomeworks] = useState([])
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [form, setForm] = useState({ title: '', questionCount: '' })
  const [createAnswerKeyFile, setCreateAnswerKeyFile] = useState(null)
  const [createTarget, setCreateTarget] = useState(() => createInitialTarget(true))

  const boardItems = useMemo(() => toHomeworkBoardItems(homeworks), [homeworks])

  const loadHomeworks = useCallback(async () => {
    const list = await fetchHomeworks(classId)
    setHomeworks(list)
  }, [classId])

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
          ? '숙제를 선택하면 상세 페이지에서 정답지·채점 결과를 관리할 수 있습니다.'
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
      ) : verifyOnly ? (
        <AssignmentVerifyList
          listTitle="숙제 목록"
          items={boardItems}
          emptyListMessage="등록된 숙제가 없습니다."
          getItemHref={(id) => `/classes/${classId}/homeworks/${id}`}
          activeId={activeHomeworkId}
        />
      ) : (
        <p className="ams-class-detail__empty">숙제 확인 탭에서 목록을 이용해 주세요.</p>
      )}
    </section>
  )
}
