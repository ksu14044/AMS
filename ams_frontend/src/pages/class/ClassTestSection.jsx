import { useCallback, useEffect, useMemo, useState } from 'react'
import { useParams } from 'react-router-dom'
import { AssignmentVerifyList } from '../../components/AssignmentVerifyBoard'
import { AnswerKeyFileField } from '../../components/AssignmentGradingModals'
import { createTest, fetchTests, toInstant, uploadTestAnswerKey } from '../../api/classesApi'
import StudentTargetPicker from '../../components/StudentTargetPicker'
import {
  buildTargetStudentIdsPayload,
  createInitialTarget,
} from '../../utils/assignmentTargets'
import { toTestBoardItems } from '../../utils/assignmentVerify'

export default function ClassTestSection({ classId, canManage, verifyOnly = false, onError }) {
  const { testId: activeTestId } = useParams()
  const [tests, setTests] = useState([])
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [form, setForm] = useState({
    title: '',
    testDate: '',
    testTime: '14:00',
    questionCount: '',
    retakeThresholdCount: '',
  })
  const [createAnswerKeyFile, setCreateAnswerKeyFile] = useState(null)
  const [createTarget, setCreateTarget] = useState(() => createInitialTarget(true))

  const boardItems = useMemo(() => toTestBoardItems(tests), [tests])

  const loadTests = useCallback(async () => {
    const list = await fetchTests(classId)
    setTests(list)
  }, [classId])

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
          ? '테스트를 선택하면 상세 페이지에서 정답지·채점·석차를 확인할 수 있습니다.'
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
      ) : verifyOnly ? (
        <AssignmentVerifyList
          listTitle="테스트 목록"
          items={boardItems}
          emptyListMessage="등록된 테스트가 없습니다."
          getItemHref={(id) => `/classes/${classId}/tests/${id}`}
          activeId={activeTestId}
        />
      ) : (
        <p className="ams-class-detail__empty">테스트 확인 탭에서 목록을 이용해 주세요.</p>
      )}
    </section>
  )
}
