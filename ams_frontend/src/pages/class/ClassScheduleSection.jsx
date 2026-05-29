import { useCallback, useEffect, useState } from 'react'
import { fetchClassSchedule, updateClassSchedule } from '../../api/classesApi'
import { DAY_OPTIONS, dayLabel } from '../../auth/dayLabels'

const EMPTY_SLOT = { dayOfWeek: 'MON', startTime: '18:00', endTime: '20:00', room: '' }

function toFormSlot(slot) {
  return {
    dayOfWeek: slot.dayOfWeek,
    startTime: slot.startTime?.slice(0, 5) ?? '18:00',
    endTime: slot.endTime?.slice(0, 5) ?? '20:00',
    room: slot.room ?? '',
  }
}

export default function ClassScheduleSection({ classId, canManage, onError }) {
  const [slots, setSlots] = useState([])
  const [formSlots, setFormSlots] = useState([])
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    onError('')
    try {
      const list = await fetchClassSchedule(classId)
      setSlots(list)
      setFormSlots(list.map(toFormSlot))
    } catch (err) {
      onError(err.message)
    } finally {
      setLoading(false)
    }
  }, [classId, onError])

  useEffect(() => {
    load()
  }, [load])

  function updateRow(index, patch) {
    setFormSlots((prev) => prev.map((row, i) => (i === index ? { ...row, ...patch } : row)))
  }

  function addRow() {
    setFormSlots((prev) => [...prev, { ...EMPTY_SLOT }])
  }

  function removeRow(index) {
    setFormSlots((prev) => prev.filter((_, i) => i !== index))
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
      <h3 className="ams-class-detail__heading">수업 정보</h3>
      <p className="ams-class-detail__hint-inline">요일·시간·강의실 (담임·관리자만 수정)</p>

      {!canManage && slots.length === 0 && (
        <p className="ams-class-detail__empty">등록된 수업 일정이 없습니다.</p>
      )}

      {!canManage && slots.length > 0 && (
        <ul className="ams-schedule-list">
          {slots.map((s) => (
            <li key={s.scheduleId} className="ams-schedule-list__item">
              <strong>{dayLabel(s.dayOfWeek)}</strong>
              <span>
                {s.startTime?.slice(0, 5)} – {s.endTime?.slice(0, 5)}
                {s.room ? ` · ${s.room}` : ''}
              </span>
            </li>
          ))}
        </ul>
      )}

      {canManage && (
        <form className="ams-schedule-form" onSubmit={handleSave}>
          {formSlots.length === 0 && (
            <p className="ams-class-detail__empty">등록된 시간대가 없습니다. 아래에서 추가하세요.</p>
          )}
          {formSlots.map((row, index) => (
            <div key={index} className="ams-schedule-form__row">
              <label>
                요일
                <select
                  value={row.dayOfWeek}
                  onChange={(e) => updateRow(index, { dayOfWeek: e.target.value })}
                >
                  {DAY_OPTIONS.map((d) => (
                    <option key={d.value} value={d.value}>
                      {d.label}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                시작
                <input
                  type="time"
                  value={row.startTime}
                  onChange={(e) => updateRow(index, { startTime: e.target.value })}
                  required
                />
              </label>
              <label>
                종료
                <input
                  type="time"
                  value={row.endTime}
                  onChange={(e) => updateRow(index, { endTime: e.target.value })}
                  required
                />
              </label>
              <label>
                강의실
                <input
                  type="text"
                  value={row.room}
                  onChange={(e) => updateRow(index, { room: e.target.value })}
                  placeholder="301"
                  maxLength={50}
                />
              </label>
              <button
                type="button"
                className="ams-btn ams-btn--ghost"
                onClick={() => removeRow(index)}
                aria-label="행 삭제"
              >
                삭제
              </button>
            </div>
          ))}
          <div className="ams-schedule-form__actions">
            <button type="button" className="ams-btn ams-btn--ghost" onClick={addRow}>
              시간대 추가
            </button>
            <button type="submit" className="ams-btn ams-btn--primary" disabled={submitting}>
              {submitting ? '저장 중…' : '일정 저장'}
            </button>
          </div>
        </form>
      )}
    </section>
  )
}
