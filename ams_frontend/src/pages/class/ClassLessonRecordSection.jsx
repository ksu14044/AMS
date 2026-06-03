import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  createLessonRecord,
  fetchClinicAssistants,
  fetchClinicPresets,
  fetchLessonRecords,
} from '../../api/classesApi'
import { AnswerKeyFileField } from '../../components/AssignmentGradingModals'
import { uploadAnswerKeysForLessonRecord } from '../../utils/answerKeyUpload'
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
  const navigate = useNavigate()
  const [records, setRecords] = useState([])
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [showWrite, setShowWrite] = useState(false)
  const [createForm, setCreateForm] = useState(EMPTY_CREATE)
  const [homeworkTarget, setHomeworkTarget] = useState(() => createInitialTarget(true))
  const [testTarget, setTestTarget] = useState(() => createInitialTarget(true))
  const [videoTarget, setVideoTarget] = useState(() => createInitialTarget(false))
  const [clinicTarget, setClinicTarget] = useState(() => createInitialTarget(true))
  const [assistants, setAssistants] = useState([])
  const [clinicPresets, setClinicPresets] = useState([])
  const [createHomeworkAnswerKeyFile, setCreateHomeworkAnswerKeyFile] = useState(null)
  const [createTestAnswerKeyFile, setCreateTestAnswerKeyFile] = useState(null)

  const load = useCallback(async () => {
    setLoading(true)
    onError('')
    try {
      const list = await fetchLessonRecords(classId)
      setRecords(list)
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

      })
      .catch((err) => onError(err.message))
  }, [classId, canEdit, onError])

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
      navigate(`/classes/${classId}/lesson-records/${created.lessonRecordId}`)
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
            수업일·요약과 숙제·테스트·영상·클리닉을 한 번에 등록합니다. 목록에서 항목을 선택하면
            상세 페이지에서 요약·귀속 항목을 확인·수정할 수 있습니다.
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

      <div className="ams-lesson-board__list ams-card ams-card--elevated ams-lesson-board__list--full">
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
                {boardRows.map((r) => (
                  <tr
                    key={r.lessonRecordId}
                    className="ams-lesson-board__row ams-lesson-board__row--link"
                    onClick={() => navigate(`/classes/${classId}/lesson-records/${r.lessonRecordId}`)}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter' || e.key === ' ') {
                        e.preventDefault()
                        navigate(`/classes/${classId}/lesson-records/${r.lessonRecordId}`)
                      }
                    }}
                    tabIndex={0}
                    role="link"
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
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </section>
  )
}
