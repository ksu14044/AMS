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

function StandaloneTestCreateForm({ classId, canManage, submitting, onSubmitting, onError, onCreated }) {
  const [form, setForm] = useState({
    title: '',
    testDate: '',
    testTime: '14:00',
    questionCount: '',
    retakeThresholdCount: '',
  })
  const [createAnswerKeyFile, setCreateAnswerKeyFile] = useState(null)
  const [createTarget, setCreateTarget] = useState(() => createInitialTarget(true))

  async function handleCreate(e) {
    e.preventDefault()
    if (!form.title.trim() || !form.testDate) return
    const questionCount = Number(form.questionCount)
    if (!questionCount || questionCount <= 0) {
      onError('문항 수를 1 이상 입력하세요.')
      return
    }
    if (createTarget.mode === 'custom' && createTarget.studentIds.length === 0) {
      onError('대상 학생을 한 명 이상 선택하세요.')
      return
    }
    onSubmitting(true)
    onError('')
    try {
      const targetStudentIds = buildTargetStudentIdsPayload(createTarget, true)
      const payload = {
        title: form.title.trim(),
        testAt: toInstant(form.testDate, form.testTime),
        questionCount,
        retakeThresholdCount: form.retakeThresholdCount
          ? Number(form.retakeThresholdCount)
          : null,
      }
      if (targetStudentIds !== undefined) {
        payload.targetStudentIds = targetStudentIds
      }
      const created = await createTest(classId, payload)
      if (createAnswerKeyFile) {
        await uploadTestAnswerKey(classId, created.testId, createAnswerKeyFile, questionCount)
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
      await onCreated()
    } catch (err) {
      onError(err.message)
    } finally {
      onSubmitting(false)
    }
  }

  if (!canManage) return null

  return (
    <div className="ams-assignment-form-wrap ams-card ams-card--elevated">
      <h4 className="ams-class-detail__subheading">중간·기말 시험 등록</h4>
      <p className="ams-class-detail__hint-inline">
        수업기록과 별도로 등록하는 시험입니다. 틀린 문항을 체크해 채점하며, 정답지 파일은 선택 사항입니다.
      </p>
      <form className="ams-assignment-form" onSubmit={handleCreate}>
        <label>
          제목
          <input
            value={form.title}
            onChange={(e) => setForm({ ...form, title: e.target.value })}
            maxLength={200}
            placeholder="예: 1학기 중간고사"
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
            required
          />
        </label>
        <label>
          합격 기준 (맞은 문항 수, 선택)
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
          {submitting ? '등록 중…' : '시험 등록'}
        </button>
      </form>
    </div>
  )
}

export default function ClassTestSection({ classId, canManage, verifyOnly = false, onError }) {
  const { testId: activeTestId } = useParams()
  const [tests, setTests] = useState([])
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)

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

  if (loading) {
    return <p className="ams-class-detail__empty">불러오는 중…</p>
  }

  return (
    <section className="ams-class-detail__section">
      <h3 className="ams-class-detail__heading">{verifyOnly ? '테스트 확인' : '테스트'}</h3>
      <p className="ams-class-detail__hint-inline">
        {verifyOnly
          ? '목록에서 시험을 선택해 채점·석차를 확인합니다. 중간·기말 시험은 아래에서 새로 등록할 수 있습니다.'
          : '정답지는 참고용이며, 채점 결과는 문항 수만 있으면 입력할 수 있습니다.'}
      </p>

      {verifyOnly && (
        <StandaloneTestCreateForm
          classId={classId}
          canManage={canManage}
          submitting={submitting}
          onSubmitting={setSubmitting}
          onError={onError}
          onCreated={loadTests}
        />
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
