import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  addLessonRecordLinkedItems,
  createLessonRecord,
  deleteLessonRecordClinicSlot,
  deleteLessonRecordHomework,
  deleteLessonRecordTest,
  deleteLessonRecordVideo,
  fetchClinicAssistants,
  fetchClinicPresets,
  fetchLessonRecord,
  fetchLessonRecords,
  updateLessonRecord,
  updateLessonRecordClinicSlot,
  updateLessonRecordHomework,
  updateLessonRecordTest,
  updateLessonRecordVideo,
} from '../../api/classesApi'
import { AnswerKeyFileField } from '../../components/AssignmentGradingModals'
import { linkedIdsByType, uploadAnswerKeysForLessonRecord } from '../../utils/answerKeyUpload'
import { dayLabel } from '../../auth/dayLabels'
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

const EMPTY_CREATE = {
  lessonDate: '',
  summary: '',
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

function formatLessonDate(dateStr) {
  if (!dateStr) return '—'
  const [y, m, d] = dateStr.split('-').map(Number)
  return `${y}.${String(m).padStart(2, '0')}.${String(d).padStart(2, '0')}`
}

function formatDateTime(iso) {
  if (!iso) return '—'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return '—'
  const y = d.getFullYear()
  const mo = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const h = String(d.getHours()).padStart(2, '0')
  const min = String(d.getMinutes()).padStart(2, '0')
  return `${y}.${mo}.${day} ${h}:${min}`
}

function linkedBadges(record) {
  const badges = []
  if (record.homeworkCount > 0) {
    badges.push({ key: 'homework', label: '숙제', count: record.homeworkCount })
  }
  if (record.testCount > 0) {
    badges.push({ key: 'test', label: '테스트', count: record.testCount })
  }
  if (record.videoCount > 0) {
    badges.push({ key: 'video', label: '영상', count: record.videoCount })
  }
  if (record.clinicCount > 0) {
    badges.push({ key: 'clinic', label: '클리닉', count: record.clinicCount })
  }
  return badges
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

function LinkedBadgeGroup({ record }) {
  const badges = linkedBadges(record)
  if (badges.length === 0) {
    return <span className="ams-lesson-board__empty-tag">없음</span>
  }
  return (
    <div className="ams-lesson-board__badges">
      {badges.map((b) => (
        <span key={b.key} className={`ams-lesson-board__badge ams-lesson-board__badge--${b.key}`}>
          {b.label}
          {b.count > 1 ? ` ${b.count}` : ''}
        </span>
      ))}
    </div>
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

export default function ClassLessonRecordSection({ classId, canEdit, onError }) {
  const [records, setRecords] = useState([])
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [selectedId, setSelectedId] = useState('')
  const [detail, setDetail] = useState(null)
  const [detailLoading, setDetailLoading] = useState(false)
  const [showWrite, setShowWrite] = useState(false)
  const [createForm, setCreateForm] = useState(EMPTY_CREATE)
  const [homeworkTarget, setHomeworkTarget] = useState(() => createInitialTarget(true))
  const [testTarget, setTestTarget] = useState(() => createInitialTarget(true))
  const [videoTarget, setVideoTarget] = useState(() => createInitialTarget(false))
  const [clinicTarget, setClinicTarget] = useState(() => createInitialTarget(true))
  const [editSummary, setEditSummary] = useState('')
  const [assistants, setAssistants] = useState([])
  const [clinicPresets, setClinicPresets] = useState([])
  const [showAddLinked, setShowAddLinked] = useState(false)
  const [addLinkedForm, setAddLinkedForm] = useState(EMPTY_ADD_LINKED)
  const [addHomeworkTarget, setAddHomeworkTarget] = useState(() => createInitialTarget(true))
  const [addTestTarget, setAddTestTarget] = useState(() => createInitialTarget(true))
  const [addVideoTarget, setAddVideoTarget] = useState(() => createInitialTarget(false))
  const [addClinicTarget, setAddClinicTarget] = useState(() => createInitialTarget(true))
  const [createHomeworkAnswerKeyFile, setCreateHomeworkAnswerKeyFile] = useState(null)
  const [createTestAnswerKeyFile, setCreateTestAnswerKeyFile] = useState(null)
  const [addHomeworkAnswerKeyFile, setAddHomeworkAnswerKeyFile] = useState(null)
  const [addTestAnswerKeyFile, setAddTestAnswerKeyFile] = useState(null)
  const [editingLinkedKey, setEditingLinkedKey] = useState('')
  const [editDraft, setEditDraft] = useState(null)
  const [editTarget, setEditTarget] = useState(() => createInitialTarget(true))

  const load = useCallback(async () => {
    setLoading(true)
    onError('')
    try {
      const list = await fetchLessonRecords(classId)
      setRecords(list)
      setSelectedId((prev) => {
        if (prev && list.some((r) => String(r.lessonRecordId) === prev)) {
          return prev
        }
        return list.length > 0 ? String(list[0].lessonRecordId) : ''
      })
    } catch (err) {
      onError(err.message)
      setRecords([])
    } finally {
      setLoading(false)
    }
  }, [classId, onError])

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
        setCreateForm((prev) => ({ ...prev, clinicPresetId: prev.clinicPresetId || defaultId }))
        setAddLinkedForm((prev) => ({ ...prev, clinicPresetId: prev.clinicPresetId || defaultId }))
      })
      .catch((err) => onError(err.message))
  }, [classId, canEdit, onError])

  useEffect(() => {
    if (!selectedId) {
      setDetail(null)
      setEditSummary('')
      setEditingLinkedKey('')
      setShowAddLinked(false)
      return
    }
    setEditingLinkedKey('')
    setShowAddLinked(false)
    let cancelled = false
    ;(async () => {
      setDetailLoading(true)
      onError('')
      try {
        const row = await fetchLessonRecord(classId, selectedId)
        if (!cancelled) {
          setDetail(row)
          setEditSummary(row.summary ?? '')
        }
      } catch (err) {
        if (!cancelled) {
          onError(err.message)
          setDetail(null)
        }
      } finally {
        if (!cancelled) setDetailLoading(false)
      }
    })()
    return () => {
      cancelled = true
    }
  }, [classId, selectedId, onError])

  const boardRows = useMemo(
    () =>
      records.map((r, index) => ({
        ...r,
        rowNo: records.length - index,
      })),
    [records],
  )

  function closeWrite() {
    setShowWrite(false)
    setCreateForm(EMPTY_CREATE)
    setHomeworkTarget(createInitialTarget(true))
    setTestTarget(createInitialTarget(true))
    setVideoTarget(createInitialTarget(false))
    setClinicTarget(createInitialTarget(true))
    setCreateHomeworkAnswerKeyFile(null)
    setCreateTestAnswerKeyFile(null)
  }

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
    if (!selectedId || !editDraft) return
    const allByDefault = item.type !== 'video'
    if (item.type !== 'video' && editTarget.mode === 'custom' && editTarget.studentIds.length === 0) {
      onError(`${linkedTypeLabel(item.type)} 대상 학생을 한 명 이상 선택하세요.`)
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
    onError('')
    try {
      let updated
      if (item.type === 'homework') {
        updated = await updateLessonRecordHomework(classId, selectedId, item.id, payload)
      } else if (item.type === 'test') {
        updated = await updateLessonRecordTest(classId, selectedId, item.id, payload)
      } else if (item.type === 'video') {
        updated = await updateLessonRecordVideo(classId, selectedId, item.id, payload)
      } else {
        updated = await updateLessonRecordClinicSlot(classId, selectedId, item.id, payload)
      }
      setDetail(updated)
      setEditSummary(updated.summary ?? '')
      await load()
      closeLinkedEdit()
    } catch (err) {
      onError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  async function handleDeleteLinked(item) {
    if (!selectedId || !item.canDelete) return
    const label = linkedTypeLabel(item.type)
    if (!window.confirm(`이 ${label} 항목을 삭제할까요?`)) return

    setSubmitting(true)
    onError('')
    try {
      let updated
      if (item.type === 'homework') {
        updated = await deleteLessonRecordHomework(classId, selectedId, item.id)
      } else if (item.type === 'test') {
        updated = await deleteLessonRecordTest(classId, selectedId, item.id)
      } else if (item.type === 'video') {
        updated = await deleteLessonRecordVideo(classId, selectedId, item.id)
      } else {
        updated = await deleteLessonRecordClinicSlot(classId, selectedId, item.id)
      }
      setDetail(updated)
      setEditSummary(updated.summary ?? '')
      await load()
      closeLinkedEdit()
    } catch (err) {
      onError(err.message)
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
    if (!selectedId) return
    if (
      !addLinkedForm.includeHomework &&
      !addLinkedForm.includeTest &&
      !addLinkedForm.includeVideo &&
      !addLinkedForm.includeClinic
    ) {
      onError('추가할 항목을 선택하세요.')
      return
    }
    if (addLinkedForm.includeHomework && !addLinkedForm.homeworkTitle.trim()) return
    if (addLinkedForm.includeHomework && !addLinkedForm.homeworkQuestionCount) return
    if (
      addLinkedForm.includeHomework &&
      addHomeworkTarget.mode === 'custom' &&
      addHomeworkTarget.studentIds.length === 0
    ) {
      onError('숙제 대상 학생을 한 명 이상 선택하세요.')
      return
    }
    if (addLinkedForm.includeTest && !addLinkedForm.testTitle.trim()) return
    if (addLinkedForm.includeTest && (!addLinkedForm.testQuestionCount || !addLinkedForm.testRetakeThresholdCount)) {
      return
    }
    if (addLinkedForm.includeTest && addTestTarget.mode === 'custom' && addTestTarget.studentIds.length === 0) {
      onError('테스트 대상 학생을 한 명 이상 선택하세요.')
      return
    }
    if (addLinkedForm.includeVideo && (!addLinkedForm.videoTitle.trim() || !addLinkedForm.youtubeUrl.trim())) {
      return
    }
    if (addLinkedForm.includeClinic) {
      if (!addLinkedForm.clinicDate || !addLinkedForm.clinicAssistantId || !addLinkedForm.clinicPresetId) return
      if (addClinicTarget.mode === 'custom' && addClinicTarget.studentIds.length === 0) {
        onError('클리닉 대상 학생을 한 명 이상 선택하세요.')
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
    onError('')
    try {
      const updated = await addLessonRecordLinkedItems(classId, selectedId, payload)
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
      onError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  async function handleCreate(e) {
    e.preventDefault()
    if (!createForm.lessonDate || !createForm.summary.trim()) return
    if (createForm.includeHomework && !createForm.homeworkTitle.trim()) return
    if (createForm.includeHomework && !createForm.homeworkQuestionCount) return
    if (createForm.includeHomework && homeworkTarget.mode === 'custom' && homeworkTarget.studentIds.length === 0) {
      onError('숙제 대상 학생을 한 명 이상 선택하세요.')
      return
    }
    if (createForm.includeTest && !createForm.testTitle.trim()) return
    if (createForm.includeTest && (!createForm.testQuestionCount || !createForm.testRetakeThresholdCount)) return
    if (createForm.includeTest && testTarget.mode === 'custom' && testTarget.studentIds.length === 0) {
      onError('테스트 대상 학생을 한 명 이상 선택하세요.')
      return
    }
    if (createForm.includeVideo && (!createForm.videoTitle.trim() || !createForm.youtubeUrl.trim())) {
      return
    }
    if (createForm.includeClinic) {
      if (!createForm.clinicDate || !createForm.clinicAssistantId || !createForm.clinicPresetId) return
      if (clinicTarget.mode === 'custom' && clinicTarget.studentIds.length === 0) {
        onError('클리닉 대상 학생을 한 명 이상 선택하세요.')
        return
      }
    }

    const payload = {
      lessonDate: createForm.lessonDate,
      summary: createForm.summary.trim(),
    }
    if (createForm.includeHomework) {
      payload.homework = appendTargetPayload(
        {
          title: createForm.homeworkTitle.trim(),
          questionCount: Number(createForm.homeworkQuestionCount),
        },
        homeworkTarget,
        true,
      )
    }
    if (createForm.includeTest) {
      payload.test = appendTargetPayload(
        {
          title: createForm.testTitle.trim(),
          questionCount: Number(createForm.testQuestionCount),
          retakeThresholdCount: Number(createForm.testRetakeThresholdCount),
        },
        testTarget,
        true,
      )
    }
    if (createForm.includeVideo) {
      payload.video = appendTargetPayload(
        {
          title: createForm.videoTitle.trim(),
          youtubeUrl: createForm.youtubeUrl.trim(),
        },
        videoTarget,
        false,
      )
    }
    if (createForm.includeClinic) {
      payload.clinic = appendTargetPayload(
        {
          clinicDate: createForm.clinicDate,
          startTime: createForm.clinicStartTime,
          assistantId: Number(createForm.clinicAssistantId),
          presetId: Number(createForm.clinicPresetId),
          maxCapacity: Number(createForm.clinicMaxCapacity) || 10,
        },
        clinicTarget,
        true,
      )
    }

    setSubmitting(true)
    onError('')
    try {
      const created = await createLessonRecord(classId, payload)
      if (createForm.includeHomework || createForm.includeTest) {
        await uploadAnswerKeysForLessonRecord(classId, created.linkedItems, {
          homeworkFile: createForm.includeHomework ? createHomeworkAnswerKeyFile : null,
          homeworkQuestionCount: createForm.homeworkQuestionCount,
          testFile: createForm.includeTest ? createTestAnswerKeyFile : null,
          testQuestionCount: createForm.testQuestionCount,
        })
      }
      closeWrite()
      await load()
      setSelectedId(String(created.lessonRecordId))
    } catch (err) {
      onError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  async function handleUpdateSummary(e) {
    e.preventDefault()
    if (!selectedId || !editSummary.trim()) return
    setSubmitting(true)
    onError('')
    try {
      const updated = await updateLessonRecord(classId, selectedId, { summary: editSummary.trim() })
      setDetail(updated)
      await load()
    } catch (err) {
      onError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) {
    return (
      <div className="ams-lesson-board ams-lesson-board--loading">
        <p className="ams-class-detail__empty">불러오는 중…</p>
      </div>
    )
  }

  return (
    <section className="ams-lesson-board">
      <header className="ams-lesson-board__header">
        <div className="ams-lesson-board__header-text">
          <div className="ams-lesson-board__title-row">
            <h3 className="ams-lesson-board__title">수업기록</h3>
            <span className="ams-pill ams-pill--muted">{records.length}건</span>
          </div>
          <p className="ams-lesson-board__lead">
            수업일·요약과 숙제·테스트·영상·클리닉을 한 번에 등록합니다. 숙제·테스트 등록 시
            정답지 파일을 함께 올릴 수 있습니다. 점수 입력은 숙제·테스트 확인 탭을 이용하세요.
          </p>
        </div>
        {canEdit && (
          <button
            type="button"
            className={`ams-btn ams-btn--primary ams-btn--sm${showWrite ? ' ams-lesson-board__write-toggle--open' : ''}`}
            onClick={() => (showWrite ? closeWrite() : setShowWrite(true))}
          >
            {showWrite ? '작성 취소' : '+ 수업기록 작성'}
          </button>
        )}
      </header>

      {canEdit && showWrite && (
        <form className="ams-lesson-board__compose ams-card ams-card--elevated" onSubmit={handleCreate}>
          <div className="ams-lesson-board__compose-head">
            <h4 className="ams-lesson-board__compose-title">새 수업기록</h4>
            <p className="ams-lesson-board__compose-desc">수업 내용과 함께 등록할 항목을 선택하세요.</p>
          </div>

          <div className="ams-lesson-board__compose-grid">
            <div className="ams-lesson-board__compose-main">
              <label className="ams-field">
                <span className="ams-field__label">수업일</span>
                <input
                  className="ams-field__input"
                  type="date"
                  value={createForm.lessonDate}
                  onChange={(e) => setCreateForm({ ...createForm, lessonDate: e.target.value })}
                  required
                />
              </label>
              <label className="ams-field">
                <span className="ams-field__label">수업 내용 요약</span>
                <textarea
                  className="ams-field__textarea"
                  value={createForm.summary}
                  onChange={(e) => setCreateForm({ ...createForm, summary: e.target.value })}
                  rows={5}
                  placeholder="오늘 수업에서 다룬 단원, 핵심 개념, 다음 수업 안내 등을 적어 주세요."
                  required
                />
              </label>
            </div>

            <div className="ams-lesson-board__compose-side">
              <p className="ams-lesson-board__compose-side-title">함께 등록할 항목</p>
              <div className="ams-lesson-board__options">
                <OptionCard
                  active={createForm.includeHomework}
                  label="숙제"
                  description="기본 반 전원 · 필요 시 학생 선택"
                  onToggle={() =>
                    setCreateForm({ ...createForm, includeHomework: !createForm.includeHomework })
                  }
                >
                  <label className="ams-field ams-field--compact">
                    <span className="ams-field__label">숙제 제목</span>
                    <input
                      className="ams-field__input"
                      value={createForm.homeworkTitle}
                      onChange={(e) =>
                        setCreateForm({ ...createForm, homeworkTitle: e.target.value })
                      }
                      maxLength={200}
                      placeholder="예: 3단원 워크북 p.42~45"
                      required
                    />
                  </label>
                  <label className="ams-field ams-field--compact">
                    <span className="ams-field__label">문항 수</span>
                    <input
                      className="ams-field__input"
                      type="number"
                      min={1}
                      value={createForm.homeworkQuestionCount}
                      onChange={(e) =>
                        setCreateForm({ ...createForm, homeworkQuestionCount: e.target.value })
                      }
                      placeholder="예: 20"
                      required
                    />
                  </label>
                  <StudentTargetPicker
                    classId={classId}
                    allByDefault
                    value={homeworkTarget}
                    onChange={setHomeworkTarget}
                    disabled={submitting}
                  />
                  <AnswerKeyFileField
                    compact
                    file={createHomeworkAnswerKeyFile}
                    disabled={submitting}
                    onFileChange={setCreateHomeworkAnswerKeyFile}
                  />
                </OptionCard>

                <OptionCard
                  active={createForm.includeTest}
                  label="테스트"
                  description="기본 반 전원 · 필요 시 학생 선택"
                  onToggle={() => setCreateForm({ ...createForm, includeTest: !createForm.includeTest })}
                >
                  <label className="ams-field ams-field--compact">
                    <span className="ams-field__label">테스트 제목</span>
                    <input
                      className="ams-field__input"
                      value={createForm.testTitle}
                      onChange={(e) => setCreateForm({ ...createForm, testTitle: e.target.value })}
                      maxLength={200}
                      placeholder="예: 3단원 형성평가"
                      required
                    />
                  </label>
                  <label className="ams-field ams-field--compact">
                    <span className="ams-field__label">문항 수</span>
                    <input
                      className="ams-field__input"
                      type="number"
                      min={1}
                      value={createForm.testQuestionCount}
                      onChange={(e) =>
                        setCreateForm({ ...createForm, testQuestionCount: e.target.value })
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
                      value={createForm.testRetakeThresholdCount}
                      onChange={(e) =>
                        setCreateForm({ ...createForm, testRetakeThresholdCount: e.target.value })
                      }
                      placeholder="이 미만이면 재시험"
                      required
                    />
                  </label>
                  <StudentTargetPicker
                    classId={classId}
                    allByDefault
                    value={testTarget}
                    onChange={setTestTarget}
                    disabled={submitting}
                  />
                  <AnswerKeyFileField
                    compact
                    file={createTestAnswerKeyFile}
                    disabled={submitting}
                    onFileChange={setCreateTestAnswerKeyFile}
                  />
                </OptionCard>

                <OptionCard
                  active={createForm.includeVideo}
                  label="영상"
                  description="전원 시청 · 인증 대상 선택"
                  onToggle={() => setCreateForm({ ...createForm, includeVideo: !createForm.includeVideo })}
                >
                  <label className="ams-field ams-field--compact">
                    <span className="ams-field__label">영상 제목</span>
                    <input
                      className="ams-field__input"
                      value={createForm.videoTitle}
                      onChange={(e) => setCreateForm({ ...createForm, videoTitle: e.target.value })}
                      maxLength={200}
                      placeholder="예: 3단원 개념 정리"
                      required
                    />
                  </label>
                  <label className="ams-field ams-field--compact">
                    <span className="ams-field__label">YouTube URL</span>
                    <input
                      className="ams-field__input"
                      type="url"
                      value={createForm.youtubeUrl}
                      onChange={(e) => setCreateForm({ ...createForm, youtubeUrl: e.target.value })}
                      placeholder="https://www.youtube.com/watch?v=..."
                      required
                    />
                  </label>
                  <StudentTargetPicker
                    classId={classId}
                    allByDefault={false}
                    label="인증 대상 학생"
                    value={videoTarget}
                    onChange={setVideoTarget}
                    disabled={submitting}
                  />
                </OptionCard>

                <OptionCard
                  active={createForm.includeClinic}
                  label="클리닉"
                  description="날짜·시간·조교 슬롯"
                  onToggle={() => {
                    const next = !createForm.includeClinic
                    setCreateForm({
                      ...createForm,
                      includeClinic: next,
                      clinicDate:
                        next && !createForm.clinicDate && createForm.lessonDate
                          ? createForm.lessonDate
                          : createForm.clinicDate,
                      clinicPresetId:
                        next && !createForm.clinicPresetId
                          ? defaultPresetId(clinicPresets)
                          : createForm.clinicPresetId,
                    })
                  }}
                >
                  <label className="ams-field ams-field--compact">
                    <span className="ams-field__label">클리닉 날짜</span>
                    <input
                      className="ams-field__input"
                      type="date"
                      value={createForm.clinicDate}
                      onChange={(e) => setCreateForm({ ...createForm, clinicDate: e.target.value })}
                      required
                    />
                  </label>
                  {createForm.clinicDate && (
                    <p className="ams-lesson-board__field-hint">
                      {dayLabel(dayOfWeekFromDate(createForm.clinicDate))}요일 클리닉
                    </p>
                  )}
                  <label className="ams-field ams-field--compact">
                    <span className="ams-field__label">시작 시각</span>
                    <input
                      className="ams-field__input"
                      type="time"
                      value={createForm.clinicStartTime}
                      onChange={(e) =>
                        setCreateForm({ ...createForm, clinicStartTime: e.target.value })
                      }
                      required
                    />
                  </label>
                  <label className="ams-field ams-field--compact">
                    <span className="ams-field__label">담당 조교</span>
                    <select
                      className="ams-field__input"
                      value={createForm.clinicAssistantId}
                      onChange={(e) =>
                        setCreateForm({ ...createForm, clinicAssistantId: e.target.value })
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
                    value={createForm.clinicPresetId}
                    onChange={(e) =>
                      setCreateForm({ ...createForm, clinicPresetId: e.target.value })
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
                      value={createForm.clinicMaxCapacity}
                      onChange={(e) =>
                        setCreateForm({ ...createForm, clinicMaxCapacity: e.target.value })
                      }
                    />
                  </label>
                  <StudentTargetPicker
                    classId={classId}
                    allByDefault
                    value={clinicTarget}
                    onChange={setClinicTarget}
                    disabled={submitting}
                  />
                </OptionCard>
              </div>
            </div>
          </div>

          <div className="ams-lesson-board__compose-foot">
            <button type="button" className="ams-btn ams-btn--ghost ams-btn--sm" onClick={closeWrite}>
              취소
            </button>
            <button type="submit" className="ams-btn ams-btn--primary ams-btn--sm" disabled={submitting}>
              {submitting ? '등록 중…' : '수업기록 등록'}
            </button>
          </div>
        </form>
      )}

      <div className="ams-lesson-board__layout">
        <div className="ams-lesson-board__list ams-card ams-card--elevated">
          <div className="ams-lesson-board__list-head">
            <h4 className="ams-lesson-board__panel-title">목록</h4>
            <span className="ams-lesson-board__list-meta">최신순</span>
          </div>

          {records.length === 0 ? (
            <div className="ams-lesson-board__empty-state">
              <p className="ams-lesson-board__empty-title">등록된 수업기록이 없습니다</p>
              <p className="ams-lesson-board__empty-desc">
                {canEdit
                  ? '「+ 수업기록 작성」으로 첫 기록을 남겨 보세요.'
                  : '담당 교직원이 수업기록을 등록하면 여기에 표시됩니다.'}
              </p>
            </div>
          ) : (
            <div className="ams-lesson-board__table-wrap">
              <table className="ams-lesson-board__table">
                <thead>
                  <tr>
                    <th scope="col" className="ams-lesson-board__col-no">
                      번호
                    </th>
                    <th scope="col">수업일</th>
                    <th scope="col">요약</th>
                    <th scope="col">귀속 항목</th>
                    <th scope="col">작성자</th>
                    <th scope="col">등록일</th>
                  </tr>
                </thead>
                <tbody>
                  {boardRows.map((r) => {
                    const active = selectedId === String(r.lessonRecordId)
                    return (
                      <tr
                        key={r.lessonRecordId}
                        className={`ams-lesson-board__row${active ? ' ams-lesson-board__row--active' : ''}`}
                        onClick={() => setSelectedId(String(r.lessonRecordId))}
                        aria-selected={active}
                      >
                        <td className="ams-lesson-board__col-no">
                          <span className="ams-lesson-board__row-no">{r.rowNo}</span>
                        </td>
                        <td className="ams-lesson-board__col-date">{formatLessonDate(r.lessonDate)}</td>
                        <td className="ams-lesson-board__summary-cell" title={r.summary}>
                          {r.summary}
                        </td>
                        <td>
                          <LinkedBadgeGroup record={r} />
                        </td>
                        <td className="ams-lesson-board__col-author">{r.authorName}</td>
                        <td className="ams-lesson-board__col-created">{formatDateTime(r.createdAt)}</td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>

        <aside className="ams-lesson-board__detail ams-card ams-card--elevated">
          {!selectedId ? (
            <div className="ams-lesson-board__empty-state ams-lesson-board__empty-state--detail">
              <p className="ams-lesson-board__empty-title">수업기록을 선택하세요</p>
              <p className="ams-lesson-board__empty-desc">왼쪽 목록에서 항목을 클릭하면 상세 내용이 표시됩니다.</p>
            </div>
          ) : detailLoading ? (
            <p className="ams-class-detail__empty">불러오는 중…</p>
          ) : detail ? (
            <>
              <div className="ams-lesson-board__detail-head">
                <time className="ams-lesson-board__detail-date">{formatLessonDate(detail.lessonDate)}</time>
                <div className="ams-lesson-board__detail-meta">
                  <span>{detail.authorName}</span>
                  <span aria-hidden>·</span>
                  <span>{formatDateTime(detail.createdAt)}</span>
                </div>
              </div>

              <LinkedBadgeGroup record={detail} />

              <div className="ams-lesson-board__detail-section">
                <h5 className="ams-lesson-board__detail-label">수업 요약</h5>
                {canEdit ? (
                  <form className="ams-lesson-board__edit-form" onSubmit={handleUpdateSummary}>
                    <textarea
                      className="ams-field__textarea ams-lesson-board__detail-textarea"
                      value={editSummary}
                      onChange={(e) => setEditSummary(e.target.value)}
                      rows={8}
                      required
                    />
                    <button
                      type="submit"
                      className="ams-btn ams-btn--primary ams-btn--sm"
                      disabled={submitting}
                    >
                      {submitting ? '저장 중…' : '요약 저장'}
                    </button>
                  </form>
                ) : (
                  <p className="ams-lesson-board__detail-body">{detail.summary}</p>
                )}
              </div>

              <div className="ams-lesson-board__detail-section">
                <div className="ams-lesson-board__detail-section-head">
                  <h5 className="ams-lesson-board__detail-label">귀속 항목</h5>
                  {canEdit && (
                    <button
                      type="button"
                      className="ams-btn ams-btn--ghost ams-btn--sm"
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
                  <form className="ams-lesson-board__add-linked" onSubmit={handleAddLinked}>
                    <p className="ams-lesson-board__compose-desc">이 수업기록에 연결할 항목을 선택하세요.</p>
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
                    <div className="ams-lesson-board__linked-edit-actions">
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
                )}

                {detail.linkedItems?.length > 0 ? (
                  <ul className="ams-lesson-board__linked-items">
                    {detail.linkedItems.map((item) => {
                      const key = linkedItemKey(item)
                      const isEditing = editingLinkedKey === key
                      return (
                        <li
                          key={key}
                          className={`ams-lesson-board__linked-item ams-lesson-board__linked-item--${item.type}${isEditing ? ' ams-lesson-board__linked-item--editing' : ''}`}
                        >
                          <div className="ams-lesson-board__linked-item-main">
                            <span className="ams-lesson-board__linked-item-type">
                              {linkedTypeLabel(item.type)}
                            </span>
                            <span className="ams-lesson-board__linked-item-title">{item.title}</span>
                            {canEdit && (
                              <div className="ams-lesson-board__linked-item-actions">
                                {item.canEdit && (
                                  <button
                                    type="button"
                                    className="ams-btn ams-btn--ghost ams-btn--sm"
                                    onClick={() =>
                                      isEditing ? closeLinkedEdit() : openLinkedEdit(item)
                                    }
                                    disabled={submitting}
                                  >
                                    {isEditing ? '닫기' : '수정'}
                                  </button>
                                )}
                                {item.canDelete && (
                                  <button
                                    type="button"
                                    className="ams-btn ams-btn--ghost ams-btn--sm ams-lesson-board__linked-delete"
                                    onClick={() => handleDeleteLinked(item)}
                                    disabled={submitting}
                                  >
                                    삭제
                                  </button>
                                )}
                              </div>
                            )}
                          </div>
                          {isEditing && editDraft && (
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
                          )}
                        </li>
                      )
                    })}
                  </ul>
                ) : (
                  !showAddLinked && <p className="ams-lesson-board__empty-tag">연결된 항목 없음</p>
                )}
              </div>
            </>
          ) : (
            <p className="ams-class-detail__empty">수업기록을 불러올 수 없습니다.</p>
          )}
        </aside>
      </div>
    </section>
  )
}
