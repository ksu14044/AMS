import { useCallback, useEffect, useMemo, useState } from 'react'
import { fetchClassSchedule, updateClassSchedule } from '../../api/classesApi'
import { DAY_OPTIONS, dayLabel } from '../../auth/dayLabels'

const EMPTY_SLOT = { dayOfWeek: 'MON', startTime: '18:00', endTime: '20:00', room: '' }

const DAY_ORDER = Object.fromEntries(DAY_OPTIONS.map((d, i) => [d.value, i]))

function toFormSlot(slot) {
  return {
    dayOfWeek: slot.dayOfWeek,
    startTime: slot.startTime?.slice(0, 5) ?? '18:00',
    endTime: slot.endTime?.slice(0, 5) ?? '20:00',
    room: slot.room ?? '',
  }
}

function copySlot(slot) {
  return { ...slot }
}

function sortByDay(slots) {
  return [...slots].sort((a, b) => (DAY_ORDER[a.dayOfWeek] ?? 99) - (DAY_ORDER[b.dayOfWeek] ?? 99))
}

function formatTimeRange(start, end) {
  const s = start?.slice(0, 5) ?? ''
  const e = end?.slice(0, 5) ?? ''
  return s && e ? `${s} – ${e}` : s || e || '—'
}

function formatSlotSummary(row) {
  const room = row.room?.trim()
  return `${dayLabel(row.dayOfWeek)} ${formatTimeRange(row.startTime, row.endTime)}${room ? ` · ${room}` : ''}`
}

function ScheduleViewCards({ slots }) {
  const sorted = useMemo(() => sortByDay(slots), [slots])
  return (
    <ul className="ams-schedule-cards">
      {sorted.map((s) => (
        <li key={s.scheduleId} className="ams-schedule-cards__item">
          <span className="ams-schedule-cards__day" aria-hidden>
            {dayLabel(s.dayOfWeek)}
          </span>
          <div className="ams-schedule-cards__body">
            <p className="ams-schedule-cards__time">{formatTimeRange(s.startTime, s.endTime)}</p>
            {s.room ? <p className="ams-schedule-cards__room">{s.room}</p> : null}
          </div>
        </li>
      ))}
    </ul>
  )
}

function ScheduleSlotForm({ draft, onChange, idPrefix = 'schedule-draft' }) {
  return (
    <div className="ams-schedule-editor__slot">
      <div className="ams-schedule-editor__grid">
        <label className="ams-schedule-editor__field">
          <span className="ams-schedule-editor__label">요일</span>
          <select
            id={`${idPrefix}-day`}
            className="ams-schedule-editor__input ams-schedule-editor__input--day"
            value={draft.dayOfWeek}
            onChange={(e) => onChange({ dayOfWeek: e.target.value })}
          >
            {DAY_OPTIONS.map((d) => (
              <option key={d.value} value={d.value}>
                {d.label}요일
              </option>
            ))}
          </select>
        </label>
        <label className="ams-schedule-editor__field">
          <span className="ams-schedule-editor__label">시작</span>
          <input
            id={`${idPrefix}-start`}
            className="ams-schedule-editor__input"
            type="time"
            value={draft.startTime}
            onChange={(e) => onChange({ startTime: e.target.value })}
            required
          />
        </label>
        <label className="ams-schedule-editor__field">
          <span className="ams-schedule-editor__label">종료</span>
          <input
            id={`${idPrefix}-end`}
            className="ams-schedule-editor__input"
            type="time"
            value={draft.endTime}
            onChange={(e) => onChange({ endTime: e.target.value })}
            required
          />
        </label>
        <label className="ams-schedule-editor__field ams-schedule-editor__field--room">
          <span className="ams-schedule-editor__label">강의실</span>
          <input
            id={`${idPrefix}-room`}
            className="ams-schedule-editor__input"
            type="text"
            value={draft.room}
            onChange={(e) => onChange({ room: e.target.value })}
            placeholder="예: 301"
            maxLength={50}
          />
        </label>
      </div>
    </div>
  )
}

function ScheduleEditor({
  formSlots,
  draft,
  editingIndex,
  submitting,
  onDraftChange,
  onAddToList,
  onApplyEdit,
  onCancelEdit,
  onEditIndex,
  onRemoveFromList,
  onSave,
}) {
  const sortedEntries = useMemo(() => {
    return formSlots
      .map((row, index) => ({ row, index }))
      .sort((a, b) => (DAY_ORDER[a.row.dayOfWeek] ?? 99) - (DAY_ORDER[b.row.dayOfWeek] ?? 99))
  }, [formSlots])

  const isEditing = editingIndex !== null

  return (
    <form className="ams-schedule-editor" onSubmit={onSave}>
      <div className="ams-schedule-editor__panel">
        <p className="ams-schedule-editor__panel-title">
          {isEditing ? '시간대 수정' : '시간대 입력'}
        </p>
        <ScheduleSlotForm draft={draft} onChange={onDraftChange} />
        <div className="ams-schedule-editor__panel-actions">
          {isEditing ? (
            <>
              <button
                type="button"
                className="ams-btn ams-btn--primary ams-btn--sm"
                disabled={submitting}
                onClick={onApplyEdit}
              >
                변경 적용
              </button>
              <button
                type="button"
                className="ams-btn ams-btn--ghost ams-btn--sm"
                disabled={submitting}
                onClick={onCancelEdit}
              >
                취소
              </button>
            </>
          ) : (
            <button
              type="button"
              className="ams-btn ams-btn--primary ams-btn--sm"
              disabled={submitting}
              onClick={onAddToList}
            >
              목록에 추가
            </button>
          )}
        </div>
      </div>

      <div className="ams-schedule-editor__roster">
        <p className="ams-schedule-editor__roster-title">
          등록된 시간대
          <span className="ams-schedule-editor__roster-count">{formSlots.length}</span>
        </p>
        {formSlots.length === 0 ? (
          <p className="ams-schedule-editor__roster-empty">위에서 입력 후 「목록에 추가」를 눌러 주세요.</p>
        ) : (
          <ul className="ams-schedule-editor__roster-list">
            {sortedEntries.map(({ row, index }) => {
              const active = editingIndex === index
              return (
                <li
                  key={index}
                  className={`ams-schedule-editor__roster-item${active ? ' ams-schedule-editor__roster-item--active' : ''}`}
                >
                  <span className="ams-schedule-editor__roster-text">{formatSlotSummary(row)}</span>
                  <span className="ams-schedule-editor__roster-btns">
                    <button
                      type="button"
                      className="ams-btn ams-btn--ghost ams-btn--sm"
                      disabled={submitting || active}
                      onClick={() => onEditIndex(index)}
                    >
                      수정
                    </button>
                    <button
                      type="button"
                      className="ams-schedule-editor__remove"
                      disabled={submitting}
                      onClick={() => onRemoveFromList(index)}
                    >
                      삭제
                    </button>
                  </span>
                </li>
              )
            })}
          </ul>
        )}
      </div>

      <footer className="ams-schedule-editor__foot">
        <button type="submit" className="ams-btn ams-btn--primary ams-btn--sm" disabled={submitting}>
          {submitting ? '저장 중…' : '일정 저장'}
        </button>
      </footer>
    </form>
  )
}

export default function ClassScheduleSection({ classId, canManage, onError, embedded = false }) {
  const [slots, setSlots] = useState([])
  const [formSlots, setFormSlots] = useState([])
  const [editorDraft, setEditorDraft] = useState(() => copySlot(EMPTY_SLOT))
  const [editingIndex, setEditingIndex] = useState(null)
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)

  const resetEditor = useCallback(() => {
    setEditorDraft(copySlot(EMPTY_SLOT))
    setEditingIndex(null)
  }, [])

  const load = useCallback(async () => {
    setLoading(true)
    onError('')
    try {
      const list = await fetchClassSchedule(classId)
      setSlots(list)
      setFormSlots(list.map(toFormSlot))
      resetEditor()
    } catch (err) {
      onError(err.message)
    } finally {
      setLoading(false)
    }
  }, [classId, onError, resetEditor])

  useEffect(() => {
    load()
  }, [load])

  function updateDraft(patch) {
    setEditorDraft((prev) => ({ ...prev, ...patch }))
  }

  function handleAddToList() {
    if (!editorDraft.startTime || !editorDraft.endTime) return
    setFormSlots((prev) => [...prev, copySlot(editorDraft)])
    resetEditor()
  }

  function handleApplyEdit() {
    if (editingIndex === null || !editorDraft.startTime || !editorDraft.endTime) return
    setFormSlots((prev) =>
      prev.map((row, i) => (i === editingIndex ? copySlot(editorDraft) : row)),
    )
    resetEditor()
  }

  function handleEditIndex(index) {
    setEditingIndex(index)
    setEditorDraft(copySlot(formSlots[index]))
  }

  function handleRemoveFromList(index) {
    setFormSlots((prev) => prev.filter((_, i) => i !== index))
    if (editingIndex === index) {
      resetEditor()
    } else if (editingIndex !== null && index < editingIndex) {
      setEditingIndex(editingIndex - 1)
    }
  }

  async function handleSave(e) {
    e.preventDefault()
    setSubmitting(true)
    onError('')
    try {
      const payload = formSlots.map((s) => ({
        dayOfWeek: s.dayOfWeek,
        startTime: s.startTime,
        endTime: s.endTime,
        room: s.room.trim() || null,
      }))
      const updated = await updateClassSchedule(classId, payload)
      setSlots(updated)
      setFormSlots(updated.map(toFormSlot))
      resetEditor()
    } catch (err) {
      onError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) {
    return (
      <p className={embedded ? 'ams-class-detail__subsection-empty' : 'ams-class-detail__empty'}>
        불러오는 중…
      </p>
    )
  }

  const Wrapper = embedded ? 'div' : 'section'
  const Heading = embedded ? 'h4' : 'h3'

  return (
    <Wrapper className={embedded ? 'ams-class-detail__subsection' : 'ams-class-detail__section'}>
      <Heading className={embedded ? 'ams-class-detail__subheading' : 'ams-class-detail__heading'}>
        수업 정보
      </Heading>
      <p className="ams-class-detail__hint-inline">
        {canManage
          ? '한 칸에 요일·시간을 입력해 목록에 추가한 뒤, 하단에서 일정을 저장하세요.'
          : '이 반의 정규 수업 요일·시간·강의실입니다.'}
      </p>

      {!canManage && slots.length === 0 && (
        <p className="ams-class-detail__empty">등록된 수업 일정이 없습니다.</p>
      )}

      {!canManage && slots.length > 0 && <ScheduleViewCards slots={slots} />}

      {canManage && (
        <ScheduleEditor
          formSlots={formSlots}
          draft={editorDraft}
          editingIndex={editingIndex}
          submitting={submitting}
          onDraftChange={updateDraft}
          onAddToList={handleAddToList}
          onApplyEdit={handleApplyEdit}
          onCancelEdit={resetEditor}
          onEditIndex={handleEditIndex}
          onRemoveFromList={handleRemoveFromList}
          onSave={handleSave}
        />
      )}
    </Wrapper>
  )
}
