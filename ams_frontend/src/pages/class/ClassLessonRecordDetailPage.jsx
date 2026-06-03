import { useCallback, useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import AssignmentDetailPageShell from '../../components/AssignmentDetailPageShell'
import {
  LessonRecordDetailHero,
  LessonRecordLinkedCard,
  LessonRecordLinkedEmpty,
  LessonRecordSummarySection,
  lessonRecordShellTitle,
} from '../../components/LessonRecordDetailParts'
import {
  addLessonRecordLinkedItems,
  deleteLessonRecordClinicSlot,
  deleteLessonRecordHomework,
  deleteLessonRecordTest,
  deleteLessonRecordVideo,
  fetchClinicAssistants,
  fetchClassDetail,
  fetchClinicPresets,
  fetchLessonRecord,
  updateLessonRecord,
  updateLessonRecordClinicSlot,
  updateLessonRecordHomework,
  updateLessonRecordTest,
  updateLessonRecordVideo,
} from '../../api/classesApi'
import { AnswerKeyFileField } from '../../components/AssignmentGradingModals'
import { linkedIdsByType, uploadAnswerKeysForLessonRecord } from '../../utils/answerKeyUpload'
import StudentTargetPicker from '../../components/StudentTargetPicker'
import { ClinicPresetPicker } from '../../components/ClinicPresetSection'
import {
  buildTargetStudentIdsPayload,
  createInitialTarget,
  targetFromResponse,
} from '../../utils/assignmentTargets'
import { defaultPresetId } from '../../utils/clinicPresets'

const JS_DAY_TO_CLINIC = ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT']

function dayOfWeekFromDate(dateStr) {
  if (!dateStr) return 'MON'
  const d = new Date(`${dateStr}T12:00:00`)
  return JS_DAY_TO_CLINIC[d.getDay()]
}

const EMPTY_ADD_LINKED = {
  includeHomework: false,
  homeworkTitle: '',
  homeworkQuestionCount: '',
  includeTest: false,
  testTitle: '',
  testQuestionCount: '',
  testRetakeThresholdCount: '',
  includeVideo: false,
  videoTitle: '',
  youtubeUrl: '',
  includeClinic: false,
  clinicDate: '',
  clinicStartTime: '18:00',
  clinicAssistantId: '',
  clinicPresetId: '',
  clinicMaxCapacity: '10',
}

const LINKED_META = {
  homework: { label: '숙제', tone: 'homework' },
  test: { label: '테스트', tone: 'test' },
  video: { label: '영상', tone: 'video' },
  clinic: { label: '클리닉', tone: 'clinic' },
}

function linkedItemKey(item) {
  return `${item.type}-${item.id}`
}

function editDraftFromItem(item) {
  if (item.type === 'homework') {
    return {
      title: item.title ?? '',
      questionCount: item.questionCount != null ? String(item.questionCount) : '',
    }
  }
  if (item.type === 'test') {
    return {
      title: item.title ?? '',
      questionCount: item.questionCount != null ? String(item.questionCount) : '',
      retakeThresholdCount:
        item.retakeThresholdCount != null ? String(item.retakeThresholdCount) : '',
    }
  }
  if (item.type === 'video') {
    return {
      title: item.title ?? '',
      youtubeUrl: item.youtubeUrl ?? '',
    }
  }
  if (item.type === 'clinic') {
    return {
      clinicDate: item.clinicDate ?? '',
      clinicStartTime: item.clinicStartTime ?? '18:00',
      clinicAssistantId: item.assistantId != null ? String(item.assistantId) : '',
      clinicPresetId: item.presetId != null ? String(item.presetId) : '',
      clinicMaxCapacity: item.maxCapacity != null ? String(item.maxCapacity) : '10',
    }
  }
  return {}
}

function linkedTypeLabel(type) {
  return LINKED_META[type]?.label ?? type
}

function LinkedItemEditForm({
  item,
  draft,
  onDraftChange,
  target,
  onTargetChange,
  classId,
  assistants,
  clinicPresets,
  submitting,
  onCancel,
  onSave,
}) {
  const metadataLocked = (item.type === 'homework' || item.type === 'test') && !item.canDelete
  const scheduleLocked = item.type === 'clinic' && !item.canDelete
  const allByDefault = item.type !== 'video'

  return (
    <form
      className="ams-lesson-board__linked-edit"
      onSubmit={(e) => {
        e.preventDefault()
        onSave()
      }}
    >
      {item.type === 'homework' && (
        <>
          <label className="ams-field ams-field--compact">
            <span className="ams-field__label">숙제 제목</span>
            <input
              className="ams-field__input"
              value={draft.title}
              onChange={(e) => onDraftChange({ ...draft, title: e.target.value })}
              maxLength={200}
              required
              disabled={metadataLocked}
            />
          </label>
          <label className="ams-field ams-field--compact">
            <span className="ams-field__label">문항 수</span>
            <input
              className="ams-field__input"
              type="number"
              min={1}
              value={draft.questionCount}
              onChange={(e) => onDraftChange({ ...draft, questionCount: e.target.value })}
              required
              disabled={metadataLocked}
            />
          </label>
          {metadataLocked && (
            <p className="ams-lesson-board__field-hint ams-lesson-board__field-hint--warn">
              완료된 숙제는 제목·문항 수를 변경할 수 없습니다.
            </p>
          )}
        </>
      )}

      {item.type === 'test' && (
        <>
          <label className="ams-field ams-field--compact">
            <span className="ams-field__label">테스트 제목</span>
            <input
              className="ams-field__input"
              value={draft.title}
              onChange={(e) => onDraftChange({ ...draft, title: e.target.value })}
              maxLength={200}
              required
              disabled={metadataLocked}
            />
          </label>
          <label className="ams-field ams-field--compact">
            <span className="ams-field__label">문항 수</span>
            <input
              className="ams-field__input"
              type="number"
              min={1}
              value={draft.questionCount}
              onChange={(e) => onDraftChange({ ...draft, questionCount: e.target.value })}
              required
              disabled={metadataLocked}
            />
          </label>
          <label className="ams-field ams-field--compact">
            <span className="ams-field__label">합격 기준 (맞은 문항 수)</span>
            <input
              className="ams-field__input"
              type="number"
              min={1}
              value={draft.retakeThresholdCount}
              onChange={(e) => onDraftChange({ ...draft, retakeThresholdCount: e.target.value })}
              required
              disabled={metadataLocked}
            />
          </label>
          {metadataLocked && (
            <p className="ams-lesson-board__field-hint ams-lesson-board__field-hint--warn">
              완료된 테스트는 제목·문항 수·합격 기준을 변경할 수 없습니다.
            </p>
          )}
        </>
      )}

      {item.type === 'video' && (
        <>
          <label className="ams-field ams-field--compact">
            <span className="ams-field__label">영상 제목</span>
            <input
              className="ams-field__input"
              value={draft.title}
              onChange={(e) => onDraftChange({ ...draft, title: e.target.value })}
              maxLength={200}
              required
            />
          </label>
          <label className="ams-field ams-field--compact">
            <span className="ams-field__label">YouTube URL</span>
            <input
              className="ams-field__input"
              type="url"
              value={draft.youtubeUrl}
              onChange={(e) => onDraftChange({ ...draft, youtubeUrl: e.target.value })}
              required
            />
          </label>
        </>
      )}

      {item.type === 'clinic' && (
        <>
          <label className="ams-field ams-field--compact">
            <span className="ams-field__label">클리닉 날짜</span>
            <input
              className="ams-field__input"
              type="date"
              value={draft.clinicDate}
              onChange={(e) => onDraftChange({ ...draft, clinicDate: e.target.value })}
              required
              disabled={scheduleLocked}
            />
          </label>
          <label className="ams-field ams-field--compact">
            <span className="ams-field__label">시작 시각</span>
            <input
              className="ams-field__input"
              type="time"
              value={draft.clinicStartTime}
              onChange={(e) => onDraftChange({ ...draft, clinicStartTime: e.target.value })}
              required
              disabled={scheduleLocked}
            />
          </label>
          {scheduleLocked && (
            <p className="ams-lesson-board__field-hint ams-lesson-board__field-hint--warn">
              예약이 있는 슬롯은 날짜·시간을 변경할 수 없습니다.
            </p>
          )}
          <label className="ams-field ams-field--compact">
            <span className="ams-field__label">담당 조교</span>
            <select
              className="ams-field__input"
              value={draft.clinicAssistantId}
              onChange={(e) => onDraftChange({ ...draft, clinicAssistantId: e.target.value })}
              required
            >
              <option value="">조교 선택</option>
              {assistants.map((a) => (
                <option key={a.userId} value={a.userId}>
                  {a.name}
                </option>
              ))}
            </select>
          </label>
          <ClinicPresetPicker
            presets={clinicPresets}
            value={draft.clinicPresetId}
            onChange={(e) => onDraftChange({ ...draft, clinicPresetId: e.target.value })}
            disabled={submitting}
          />
          <label className="ams-field ams-field--compact">
            <span className="ams-field__label">정원</span>
            <input
              className="ams-field__input"
              type="number"
              min={1}
              max={20}
              value={draft.clinicMaxCapacity}
              onChange={(e) => onDraftChange({ ...draft, clinicMaxCapacity: e.target.value })}
            />
          </label>
        </>
      )}

      {(item.type === 'homework' ||
        item.type === 'test' ||
        item.type === 'video' ||
        item.type === 'clinic') && (
        <StudentTargetPicker
          classId={classId}
          allByDefault={allByDefault}
          label={item.type === 'video' ? '인증 대상 학생' : undefined}
          value={target}
          onChange={onTargetChange}
          disabled={submitting}
        />
      )}

      <div className="ams-lesson-board__linked-edit-actions">
        <button type="button" className="ams-btn ams-btn--ghost ams-btn--sm" onClick={onCancel}>
          취소
        </button>
        <button type="submit" className="ams-btn ams-btn--primary ams-btn--sm" disabled={submitting}>
          {submitting ? '저장 중…' : '저장'}
        </button>
      </div>
    </form>
  )
}

function OptionCard({ active, label, description, onToggle, children }) {
  return (
    <div className={`ams-lesson-board__option${active ? ' ams-lesson-board__option--active' : ''}`}>
      <button type="button" className="ams-lesson-board__option-head" onClick={onToggle}>
        <span className="ams-lesson-board__option-check" aria-hidden />
        <span className="ams-lesson-board__option-text">
          <strong>{label}</strong>
          <span>{description}</span>
        </span>
      </button>
      {active && children && <div className="ams-lesson-board__option-body">{children}</div>}
    </div>
  )
}

export default function ClassLessonRecordDetailPage() {
  const { classId, lessonRecordId } = useParams()
  const [classDetail, setClassDetail] = useState(null)
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [detail, setDetail] = useState(null)
  const [editSummary, setEditSummary] = useState('')
  const [assistants, setAssistants] = useState([])
  const [clinicPresets, setClinicPresets] = useState([])
  const [showAddLinked, setShowAddLinked] = useState(false)
  const [addLinkedForm, setAddLinkedForm] = useState(EMPTY_ADD_LINKED)
  const [addHomeworkTarget, setAddHomeworkTarget] = useState(() => createInitialTarget(true))
  const [addTestTarget, setAddTestTarget] = useState(() => createInitialTarget(true))
  const [addVideoTarget, setAddVideoTarget] = useState(() => createInitialTarget(false))
  const [addClinicTarget, setAddClinicTarget] = useState(() => createInitialTarget(true))
  const [addHomeworkAnswerKeyFile, setAddHomeworkAnswerKeyFile] = useState(null)
  const [addTestAnswerKeyFile, setAddTestAnswerKeyFile] = useState(null)
  const [editingLinkedKey, setEditingLinkedKey] = useState('')
  const [editDraft, setEditDraft] = useState(null)
  const [editTarget, setEditTarget] = useState(() => createInitialTarget(true))

  const canEdit = classDetail?.canEditContent ?? false

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    setEditingLinkedKey('')
    setShowAddLinked(false)
    try {
      const [detailRow, classRow] = await Promise.all([
        fetchLessonRecord(classId, lessonRecordId),
        fetchClassDetail(classId),
      ])
      setDetail(detailRow)
      setEditSummary(detailRow.summary ?? '')
      setClassDetail(classRow)
    } catch (err) {
      setError(err.message)
      setDetail(null)
    } finally {
      setLoading(false)
    }
  }, [classId, lessonRecordId])

  useEffect(() => {
    load()
  }, [load])

  useEffect(() => {
    if (!canEdit) return
    Promise.all([fetchClinicAssistants(classId), fetchClinicPresets(classId)])
      .then(([assistantList, presetList]) => {
        setAssistants(assistantList)
        setClinicPresets(presetList)
        const defaultId = defaultPresetId(presetList)
        setAddLinkedForm((prev) => ({ ...prev, clinicPresetId: prev.clinicPresetId || defaultId }))
      })
      .catch((err) => setError(err.message))
  }, [classId, canEdit])

  function appendTargetPayload(item, target, allByDefault) {
    const targetStudentIds = buildTargetStudentIdsPayload(target, allByDefault)
    if (targetStudentIds !== undefined) {
      item.targetStudentIds = targetStudentIds
    }
    return item
  }

  function closeLinkedEdit() {
    setEditingLinkedKey('')
    setEditDraft(null)
  }

  function openLinkedEdit(item) {
    setShowAddLinked(false)
    setEditingLinkedKey(linkedItemKey(item))
    setEditDraft(editDraftFromItem(item))
    setEditTarget(targetFromResponse(item.targets, item.type !== 'video'))
  }

  async function handleSaveLinkedEdit(item) {
    if (!lessonRecordId || !editDraft) return
    const allByDefault = item.type !== 'video'
    if (item.type !== 'video' && editTarget.mode === 'custom' && editTarget.studentIds.length === 0) {
      setError(`${linkedTypeLabel(item.type)} 대상 학생을 한 명 이상 선택하세요.`)
      return
    }

    let payload
    if (item.type === 'homework') {
      payload = appendTargetPayload(
        { title: editDraft.title.trim(), questionCount: Number(editDraft.questionCount) },
        editTarget,
        true,
      )
    } else if (item.type === 'test') {
      payload = appendTargetPayload(
        {
          title: editDraft.title.trim(),
          questionCount: Number(editDraft.questionCount),
          retakeThresholdCount: Number(editDraft.retakeThresholdCount),
        },
        editTarget,
        true,
      )
    } else if (item.type === 'video') {
      payload = appendTargetPayload(
        { title: editDraft.title.trim(), youtubeUrl: editDraft.youtubeUrl.trim() },
        editTarget,
        false,
      )
    } else if (item.type === 'clinic') {
      if (!editDraft.clinicDate || !editDraft.clinicAssistantId || !editDraft.clinicPresetId) return
      payload = appendTargetPayload(
        {
          clinicDate: editDraft.clinicDate,
          startTime: editDraft.clinicStartTime,
          assistantId: Number(editDraft.clinicAssistantId),
          presetId: Number(editDraft.clinicPresetId),
          maxCapacity: Number(editDraft.clinicMaxCapacity) || 10,
        },
        editTarget,
        true,
      )
    } else {
      return
    }

    setSubmitting(true)
    setError('')
    try {
      let updated
      if (item.type === 'homework') {
        updated = await updateLessonRecordHomework(classId, lessonRecordId, item.id, payload)
      } else if (item.type === 'test') {
        updated = await updateLessonRecordTest(classId, lessonRecordId, item.id, payload)
      } else if (item.type === 'video') {
        updated = await updateLessonRecordVideo(classId, lessonRecordId, item.id, payload)
      } else {
        updated = await updateLessonRecordClinicSlot(classId, lessonRecordId, item.id, payload)
      }
      setDetail(updated)
      setEditSummary(updated.summary ?? '')
      await load()
      closeLinkedEdit()
    } catch (err) {
      setError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  async function handleDeleteLinked(item) {
    if (!lessonRecordId || !item.canDelete) return
    const label = linkedTypeLabel(item.type)
    if (!window.confirm(`이 ${label} 항목을 삭제할까요?`)) return

    setSubmitting(true)
    setError('')
    try {
      let updated
      if (item.type === 'homework') {
        updated = await deleteLessonRecordHomework(classId, lessonRecordId, item.id)
      } else if (item.type === 'test') {
        updated = await deleteLessonRecordTest(classId, lessonRecordId, item.id)
      } else if (item.type === 'video') {
        updated = await deleteLessonRecordVideo(classId, lessonRecordId, item.id)
      } else {
        updated = await deleteLessonRecordClinicSlot(classId, lessonRecordId, item.id)
      }
      setDetail(updated)
      setEditSummary(updated.summary ?? '')
      await load()
      closeLinkedEdit()
    } catch (err) {
      setError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  function closeAddLinked() {
    setShowAddLinked(false)
    setAddLinkedForm(EMPTY_ADD_LINKED)
    setAddHomeworkTarget(createInitialTarget(true))
    setAddTestTarget(createInitialTarget(true))
    setAddVideoTarget(createInitialTarget(false))
    setAddClinicTarget(createInitialTarget(true))
    setAddHomeworkAnswerKeyFile(null)
    setAddTestAnswerKeyFile(null)
  }

  async function handleAddLinked(e) {
    e.preventDefault()
    if (!lessonRecordId) return
    if (
      !addLinkedForm.includeHomework &&
      !addLinkedForm.includeTest &&
      !addLinkedForm.includeVideo &&
      !addLinkedForm.includeClinic
    ) {
      setError('추가할 항목을 선택하세요.')
      return
    }
    if (addLinkedForm.includeHomework && !addLinkedForm.homeworkTitle.trim()) return
    if (addLinkedForm.includeHomework && !addLinkedForm.homeworkQuestionCount) return
    if (
      addLinkedForm.includeHomework &&
      addHomeworkTarget.mode === 'custom' &&
      addHomeworkTarget.studentIds.length === 0
    ) {
      setError('숙제 대상 학생을 한 명 이상 선택하세요.')
      return
    }
    if (addLinkedForm.includeTest && !addLinkedForm.testTitle.trim()) return
    if (addLinkedForm.includeTest && (!addLinkedForm.testQuestionCount || !addLinkedForm.testRetakeThresholdCount)) {
      return
    }
    if (addLinkedForm.includeTest && addTestTarget.mode === 'custom' && addTestTarget.studentIds.length === 0) {
      setError('테스트 대상 학생을 한 명 이상 선택하세요.')
      return
    }
    if (addLinkedForm.includeVideo && (!addLinkedForm.videoTitle.trim() || !addLinkedForm.youtubeUrl.trim())) {
      return
    }
    if (addLinkedForm.includeClinic) {
      if (!addLinkedForm.clinicDate || !addLinkedForm.clinicAssistantId || !addLinkedForm.clinicPresetId) return
      if (addClinicTarget.mode === 'custom' && addClinicTarget.studentIds.length === 0) {
        setError('클리닉 대상 학생을 한 명 이상 선택하세요.')
        return
      }
    }

    const payload = {}
    if (addLinkedForm.includeHomework) {
      payload.homework = appendTargetPayload(
        {
          title: addLinkedForm.homeworkTitle.trim(),
          questionCount: Number(addLinkedForm.homeworkQuestionCount),
        },
        addHomeworkTarget,
        true,
      )
    }
    if (addLinkedForm.includeTest) {
      payload.test = appendTargetPayload(
        {
          title: addLinkedForm.testTitle.trim(),
          questionCount: Number(addLinkedForm.testQuestionCount),
          retakeThresholdCount: Number(addLinkedForm.testRetakeThresholdCount),
        },
        addTestTarget,
        true,
      )
    }
    if (addLinkedForm.includeVideo) {
      payload.video = appendTargetPayload(
        {
          title: addLinkedForm.videoTitle.trim(),
          youtubeUrl: addLinkedForm.youtubeUrl.trim(),
        },
        addVideoTarget,
        false,
      )
    }
    if (addLinkedForm.includeClinic) {
      payload.clinic = appendTargetPayload(
        {
          clinicDate: addLinkedForm.clinicDate,
          startTime: addLinkedForm.clinicStartTime,
          assistantId: Number(addLinkedForm.clinicAssistantId),
          presetId: Number(addLinkedForm.clinicPresetId),
          maxCapacity: Number(addLinkedForm.clinicMaxCapacity) || 10,
        },
        addClinicTarget,
        true,
      )
    }

    const beforeHomeworkIds = new Set(linkedIdsByType(detail?.linkedItems, 'homework'))
    const beforeTestIds = new Set(linkedIdsByType(detail?.linkedItems, 'test'))

    setSubmitting(true)
    setError('')
    try {
      const updated = await addLessonRecordLinkedItems(classId, lessonRecordId, payload)
      if (addLinkedForm.includeHomework || addLinkedForm.includeTest) {
        await uploadAnswerKeysForLessonRecord(
          classId,
          updated.linkedItems,
          {
            homeworkFile: addLinkedForm.includeHomework ? addHomeworkAnswerKeyFile : null,
            homeworkQuestionCount: addLinkedForm.homeworkQuestionCount,
            testFile: addLinkedForm.includeTest ? addTestAnswerKeyFile : null,
            testQuestionCount: addLinkedForm.testQuestionCount,
          },
          { homework: beforeHomeworkIds, test: beforeTestIds },
        )
      }
      setDetail(updated)
      setEditSummary(updated.summary ?? '')
      await load()
      closeAddLinked()
    } catch (err) {
      setError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  async function handleUpdateSummary(e) {
    e.preventDefault()
    if (!lessonRecordId || !editSummary.trim()) return
    setSubmitting(true)
    setError('')
    try {
      const updated = await updateLessonRecord(classId, lessonRecordId, { summary: editSummary.trim() })
      setDetail(updated)
      await load()
    } catch (err) {
      setError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  const pageTitle = detail ? lessonRecordShellTitle(detail) : ''
  const linkedCount = detail?.linkedItems?.length ?? 0

  if (loading) {
    return (
      <AssignmentDetailPageShell
        classId={classId}
        listTab="lessons"
        listLabel="수업기록"
        error={error}
      >
        <p className="ams-class-detail__empty">불러오는 중…</p>
      </AssignmentDetailPageShell>
    )
  }

  if (!detail) {
    return (
      <AssignmentDetailPageShell
        classId={classId}
        className={classDetail?.name}
        classSubject={classDetail?.subject}
        classRoom={classDetail?.classroom}
        listTab="lessons"
        listLabel="수업기록"
        error={error || '수업기록을 찾을 수 없습니다.'}
      />
    )
  }

  return (
    <AssignmentDetailPageShell
      classId={classId}
      className={classDetail?.name}
      classSubject={classDetail?.subject}
      classRoom={classDetail?.classroom}
      listTab="lessons"
      listLabel="수업기록"
      title={pageTitle}
      error={error}
    >
      <main className="ams-lesson-record-detail ams-card ams-card--elevated">
        <LessonRecordDetailHero detail={detail} />

        <div className="ams-lesson-record-detail__body">
          <div className="ams-lesson-record-detail__col ams-lesson-record-detail__col--summary">
            <LessonRecordSummarySection
              detail={detail}
              canEdit={canEdit}
              editSummary={editSummary}
              onEditSummaryChange={setEditSummary}
              onSubmit={handleUpdateSummary}
              submitting={submitting}
            />
          </div>

          <div className="ams-lesson-record-detail__col ams-lesson-record-detail__col--linked">
            <section
              className="ams-lesson-record-detail__section ams-lesson-record-detail__section--linked"
              aria-labelledby="lesson-linked-heading"
            >
          <div className="ams-lesson-record-detail__section-head">
            <div className="ams-lesson-record-detail__section-head-text">
              <h2 id="lesson-linked-heading" className="ams-lesson-record-detail__section-title">
                귀속 항목
                {linkedCount > 0 ? (
                  <span className="ams-lesson-record-detail__section-count">{linkedCount}</span>
                ) : null}
              </h2>
              <p className="ams-lesson-record-detail__section-hint">
                숙제·테스트 점수 입력은 반 상세의 「숙제 확인」「테스트 확인」 탭에서 할 수 있습니다.
              </p>
            </div>
            {canEdit && (
              <button
                type="button"
                className={`ams-btn ams-btn--sm${showAddLinked ? ' ams-btn--ghost' : ' ams-btn--primary'}`}
                onClick={() => {
                  closeLinkedEdit()
                  setShowAddLinked((v) => !v)
                  if (showAddLinked) closeAddLinked()
                  else {
                    setAddLinkedForm((f) => ({
                      ...EMPTY_ADD_LINKED,
                      clinicDate: detail.lessonDate ?? '',
                    }))
                  }
                }}
              >
                {showAddLinked ? '추가 취소' : '+ 항목 추가'}
              </button>
            )}
          </div>

          {canEdit && showAddLinked && (
            <div className="ams-lesson-record-detail__add-panel">
              <h3 className="ams-lesson-record-detail__add-panel-title">항목 추가</h3>
              <p className="ams-lesson-record-detail__add-panel-desc">
                이 수업기록에 연결할 숙제·테스트·영상·클리닉을 선택하세요.
              </p>
              <form className="ams-lesson-record-detail__add-form" onSubmit={handleAddLinked}>
                    <div className="ams-lesson-board__options">
                      <OptionCard
                        active={addLinkedForm.includeHomework}
                        label="숙제"
                        description="기본 반 전원 · 필요 시 학생 선택"
                        onToggle={() =>
                          setAddLinkedForm({
                            ...addLinkedForm,
                            includeHomework: !addLinkedForm.includeHomework,
                          })
                        }
                      >
                        <label className="ams-field ams-field--compact">
                          <span className="ams-field__label">숙제 제목</span>
                          <input
                            className="ams-field__input"
                            value={addLinkedForm.homeworkTitle}
                            onChange={(e) =>
                              setAddLinkedForm({ ...addLinkedForm, homeworkTitle: e.target.value })
                            }
                            maxLength={200}
                            required
                          />
                        </label>
                        <label className="ams-field ams-field--compact">
                          <span className="ams-field__label">문항 수</span>
                          <input
                            className="ams-field__input"
                            type="number"
                            min={1}
                            value={addLinkedForm.homeworkQuestionCount}
                            onChange={(e) =>
                              setAddLinkedForm({
                                ...addLinkedForm,
                                homeworkQuestionCount: e.target.value,
                              })
                            }
                            required
                          />
                        </label>
                        <StudentTargetPicker
                          classId={classId}
                          allByDefault
                          value={addHomeworkTarget}
                          onChange={setAddHomeworkTarget}
                          disabled={submitting}
                        />
                        <AnswerKeyFileField
                          compact
                          file={addHomeworkAnswerKeyFile}
                          disabled={submitting}
                          onFileChange={setAddHomeworkAnswerKeyFile}
                        />
                      </OptionCard>

                      <OptionCard
                        active={addLinkedForm.includeTest}
                        label="테스트"
                        description="기본 반 전원 · 필요 시 학생 선택"
                        onToggle={() =>
                          setAddLinkedForm({ ...addLinkedForm, includeTest: !addLinkedForm.includeTest })
                        }
                      >
                        <label className="ams-field ams-field--compact">
                          <span className="ams-field__label">테스트 제목</span>
                          <input
                            className="ams-field__input"
                            value={addLinkedForm.testTitle}
                            onChange={(e) =>
                              setAddLinkedForm({ ...addLinkedForm, testTitle: e.target.value })
                            }
                            maxLength={200}
                            required
                          />
                        </label>
                        <label className="ams-field ams-field--compact">
                          <span className="ams-field__label">문항 수</span>
                          <input
                            className="ams-field__input"
                            type="number"
                            min={1}
                            value={addLinkedForm.testQuestionCount}
                            onChange={(e) =>
                              setAddLinkedForm({ ...addLinkedForm, testQuestionCount: e.target.value })
                            }
                            required
                          />
                        </label>
                        <label className="ams-field ams-field--compact">
                          <span className="ams-field__label">합격 기준 (맞은 문항 수)</span>
                          <input
                            className="ams-field__input"
                            type="number"
                            min={1}
                            value={addLinkedForm.testRetakeThresholdCount}
                            onChange={(e) =>
                              setAddLinkedForm({
                                ...addLinkedForm,
                                testRetakeThresholdCount: e.target.value,
                              })
                            }
                            required
                          />
                        </label>
                        <StudentTargetPicker
                          classId={classId}
                          allByDefault
                          value={addTestTarget}
                          onChange={setAddTestTarget}
                          disabled={submitting}
                        />
                        <AnswerKeyFileField
                          compact
                          file={addTestAnswerKeyFile}
                          disabled={submitting}
                          onFileChange={setAddTestAnswerKeyFile}
                        />
                      </OptionCard>

                      <OptionCard
                        active={addLinkedForm.includeVideo}
                        label="영상"
                        description="전원 시청 · 인증 대상 선택"
                        onToggle={() =>
                          setAddLinkedForm({ ...addLinkedForm, includeVideo: !addLinkedForm.includeVideo })
                        }
                      >
                        <label className="ams-field ams-field--compact">
                          <span className="ams-field__label">영상 제목</span>
                          <input
                            className="ams-field__input"
                            value={addLinkedForm.videoTitle}
                            onChange={(e) =>
                              setAddLinkedForm({ ...addLinkedForm, videoTitle: e.target.value })
                            }
                            maxLength={200}
                            required
                          />
                        </label>
                        <label className="ams-field ams-field--compact">
                          <span className="ams-field__label">YouTube URL</span>
                          <input
                            className="ams-field__input"
                            type="url"
                            value={addLinkedForm.youtubeUrl}
                            onChange={(e) =>
                              setAddLinkedForm({ ...addLinkedForm, youtubeUrl: e.target.value })
                            }
                            required
                          />
                        </label>
                        <StudentTargetPicker
                          classId={classId}
                          allByDefault={false}
                          label="인증 대상 학생"
                          value={addVideoTarget}
                          onChange={setAddVideoTarget}
                          disabled={submitting}
                        />
                      </OptionCard>

                      <OptionCard
                        active={addLinkedForm.includeClinic}
                        label="클리닉"
                        description="날짜·시간·조교 슬롯"
                        onToggle={() =>
                          setAddLinkedForm({
                            ...addLinkedForm,
                            includeClinic: !addLinkedForm.includeClinic,
                            clinicDate: addLinkedForm.clinicDate || detail.lessonDate || '',
                          })
                        }
                      >
                        <label className="ams-field ams-field--compact">
                          <span className="ams-field__label">클리닉 날짜</span>
                          <input
                            className="ams-field__input"
                            type="date"
                            value={addLinkedForm.clinicDate}
                            onChange={(e) =>
                              setAddLinkedForm({ ...addLinkedForm, clinicDate: e.target.value })
                            }
                            required
                          />
                        </label>
                        <label className="ams-field ams-field--compact">
                          <span className="ams-field__label">시작 시각</span>
                          <input
                            className="ams-field__input"
                            type="time"
                            value={addLinkedForm.clinicStartTime}
                            onChange={(e) =>
                              setAddLinkedForm({ ...addLinkedForm, clinicStartTime: e.target.value })
                            }
                            required
                          />
                        </label>
                        <label className="ams-field ams-field--compact">
                          <span className="ams-field__label">담당 조교</span>
                          <select
                            className="ams-field__input"
                            value={addLinkedForm.clinicAssistantId}
                            onChange={(e) =>
                              setAddLinkedForm({ ...addLinkedForm, clinicAssistantId: e.target.value })
                            }
                            required
                          >
                            <option value="">조교 선택</option>
                            {assistants.map((a) => (
                              <option key={a.userId} value={a.userId}>
                                {a.name}
                              </option>
                            ))}
                          </select>
                        </label>
                        <ClinicPresetPicker
                          presets={clinicPresets}
                          value={addLinkedForm.clinicPresetId}
                          onChange={(e) =>
                            setAddLinkedForm({ ...addLinkedForm, clinicPresetId: e.target.value })
                          }
                          disabled={submitting}
                        />
                        <label className="ams-field ams-field--compact">
                          <span className="ams-field__label">정원</span>
                          <input
                            className="ams-field__input"
                            type="number"
                            min={1}
                            max={20}
                            value={addLinkedForm.clinicMaxCapacity}
                            onChange={(e) =>
                              setAddLinkedForm({ ...addLinkedForm, clinicMaxCapacity: e.target.value })
                            }
                          />
                        </label>
                        <StudentTargetPicker
                          classId={classId}
                          allByDefault
                          value={addClinicTarget}
                          onChange={setAddClinicTarget}
                          disabled={submitting}
                        />
                      </OptionCard>
                    </div>
                <div className="ams-lesson-record-detail__add-foot">
                  <button
                    type="button"
                    className="ams-btn ams-btn--ghost ams-btn--sm"
                    onClick={closeAddLinked}
                  >
                    취소
                  </button>
                  <button
                    type="submit"
                    className="ams-btn ams-btn--primary ams-btn--sm"
                    disabled={submitting}
                  >
                    {submitting ? '추가 중…' : '항목 추가'}
                  </button>
                </div>
              </form>
            </div>
          )}

          {linkedCount > 0 ? (
            <ul className="ams-lesson-record-detail__linked-list">
              {detail.linkedItems.map((item) => {
                const key = linkedItemKey(item)
                const isEditing = editingLinkedKey === key
                return (
                  <LessonRecordLinkedCard
                    key={key}
                    item={item}
                    classId={classId}
                    canEdit={canEdit}
                    isEditing={isEditing}
                    submitting={submitting}
                    onEdit={() => (isEditing ? closeLinkedEdit() : openLinkedEdit(item))}
                    onDelete={() => handleDeleteLinked(item)}
                    editForm={
                      isEditing && editDraft ? (
                        <LinkedItemEditForm
                          item={item}
                          draft={editDraft}
                          onDraftChange={setEditDraft}
                          target={editTarget}
                          onTargetChange={setEditTarget}
                          classId={classId}
                          assistants={assistants}
                          clinicPresets={clinicPresets}
                          submitting={submitting}
                          onCancel={closeLinkedEdit}
                          onSave={() => handleSaveLinkedEdit(item)}
                        />
                      ) : null
                    }
                  />
                )
              })}
            </ul>
          ) : !showAddLinked ? (
            <LessonRecordLinkedEmpty canEdit={canEdit} />
          ) : null}
            </section>
          </div>
        </div>
      </main>
    </AssignmentDetailPageShell>
  )
}
