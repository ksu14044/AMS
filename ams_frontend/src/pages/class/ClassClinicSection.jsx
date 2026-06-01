import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  cancelClinicReservation,
  createClinicPreset,
  createClinicSlot,
  deleteClinicPreset,
  deleteClinicSlot,
  fetchClinicAssistants,
  fetchClinicPresets,
  fetchClinicWeek,
  reserveClinicSlot,
  updateClinicPreset,
  updateClinicReservationResult,
  updateClinicSlot,
} from '../../api/classesApi'
import { useAuth } from '../../auth/AuthContext'
import { CLINIC_DAY_OPTIONS, dayLabel } from '../../auth/dayLabels'
import ClinicPresetSection, { ClinicPresetPicker } from '../../components/ClinicPresetSection'
import StudentTargetPicker from '../../components/StudentTargetPicker'
import {
  buildTargetStudentIdsPayload,
  createInitialTarget,
} from '../../utils/assignmentTargets'
import { defaultPresetId } from '../../utils/clinicPresets'
import { addDays, mondayOfWeek } from '../../utils/weekDate'
import ClinicStaffCalendar from './ClinicStaffCalendar'
import { pickDefaultClinicDay, summarizeClinicWeekByDay } from '../../utils/clinicWeekCalendar'

const EMPTY_FORM = {
  dayOfWeek: 'MON',
  startTime: '18:00',
  assistantId: '',
  presetId: '',
  maxCapacity: '10',
}

const WEEKDAY_ORDER = ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN']

export default function ClassClinicSection({
  classId,
  canManage,
  isStudent,
  canViewResults,
  mySlotsOnly = false,
  initialClinicDay,
  onError,
}) {
  const { user } = useAuth()
  const [weekStart, setWeekStart] = useState(() => mondayOfWeek())
  const [weekView, setWeekView] = useState(null)
  const [assistants, setAssistants] = useState([])
  const [presets, setPresets] = useState([])
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [form, setForm] = useState(EMPTY_FORM)
  const [createTarget, setCreateTarget] = useState(() => createInitialTarget(true))
  const [editingId, setEditingId] = useState(null)
  const [editForm, setEditForm] = useState(EMPTY_FORM)
  const [resultDraft, setResultDraft] = useState({})
  // 학생 모드: 임시 선택(체크) 슬롯 — 명시적 "예약 저장" 후에 서버에 반영
  const [pendingSlotIds, setPendingSlotIds] = useState(() => new Set())
  const [selectedDay, setSelectedDay] = useState(
    () => initialClinicDay ?? 'MON',
  )

  const loadWeek = useCallback(async () => {
    onError('')
    try {
      const w = await fetchClinicWeek(classId, weekStart)
      setWeekView(w)
      const mine = new Set(
        (w?.slots ?? []).filter((r) => r.myReservationId).map((r) => r.slot.slotId),
      )
      setPendingSlotIds(mine)
    } catch (err) {
      onError(err.message)
    }
  }, [classId, weekStart, onError])

  const load = useCallback(async () => {
    setLoading(true)
    onError('')
    try {
      await loadWeek()
      if (canManage) {
        const [assistantList, presetList] = await Promise.all([
          fetchClinicAssistants(classId),
          fetchClinicPresets(classId),
        ])
        setAssistants(assistantList)
        setPresets(presetList)
        const defaultId = defaultPresetId(presetList)
        setForm((prev) => ({ ...prev, presetId: prev.presetId || defaultId }))
      }
    } catch (err) {
      onError(err.message)
    } finally {
      setLoading(false)
    }
  }, [canManage, classId, loadWeek, onError])

  useEffect(() => {
    load()
  }, [load])

  useEffect(() => {
    if (isStudent || !weekView?.slots) return
    const summaries = summarizeClinicWeekByDay(weekView.slots, weekStart)
    setSelectedDay((prev) => {
      if (
        initialClinicDay &&
        summaries.some((s) => s.day === initialClinicDay && s.slotCount > 0)
      ) {
        return initialClinicDay
      }
      if (summaries.some((s) => s.day === prev)) return prev
      return pickDefaultClinicDay(summaries)
    })
  }, [weekStart, weekView, isStudent, initialClinicDay])

  function assistantLabel(id, name) {
    if (name) return name
    if (!id) return '—'
    const a = assistants.find((x) => x.userId === id)
    return a ? a.name : `#${id}`
  }

  async function handleCreate(e) {
    e.preventDefault()
    if (!form.assistantId || !form.presetId) return
    if (createTarget.mode === 'custom' && createTarget.studentIds.length === 0) {
      onError('대상 학생을 한 명 이상 선택하세요.')
      return
    }
    setSubmitting(true)
    onError('')
    try {
      const payload = {
        weekStartDate: weekStart,
        dayOfWeek: form.dayOfWeek,
        startTime: form.startTime,
        assistantId: Number(form.assistantId),
        presetId: Number(form.presetId),
        maxCapacity: Number(form.maxCapacity) || 10,
      }
      const targetStudentIds = buildTargetStudentIdsPayload(createTarget, true)
      if (targetStudentIds !== undefined) {
        payload.targetStudentIds = targetStudentIds
      }
      await createClinicSlot(classId, payload)
      setForm({ ...EMPTY_FORM, presetId: defaultPresetId(presets) })
      setCreateTarget(createInitialTarget(true))
      setSelectedDay(form.dayOfWeek)
      await loadWeek()
    } catch (err) {
      onError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  function startEdit(slot) {
    setEditingId(slot.slotId)
    setEditForm({
      dayOfWeek: slot.dayOfWeek,
      startTime: slot.startTime?.slice(0, 5) ?? '18:00',
      assistantId: slot.assistantId ? String(slot.assistantId) : '',
      presetId: slot.presetId ? String(slot.presetId) : defaultPresetId(presets),
      maxCapacity: String(slot.maxCapacity ?? 10),
    })
  }

  function cancelEdit() {
    setEditingId(null)
    setEditForm(EMPTY_FORM)
  }

  async function handleUpdate(e) {
    e.preventDefault()
    if (!editingId || !editForm.assistantId || !editForm.presetId) return
    setSubmitting(true)
    onError('')
    try {
      await updateClinicSlot(classId, editingId, {
        dayOfWeek: editForm.dayOfWeek,
        startTime: editForm.startTime,
        assistantId: Number(editForm.assistantId),
        presetId: Number(editForm.presetId),
        maxCapacity: Number(editForm.maxCapacity) || 10,
      })
      cancelEdit()
      await loadWeek()
    } catch (err) {
      onError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  async function handleDelete(slotId) {
    if (!window.confirm('이 슬롯을 삭제할까요?')) return
    setSubmitting(true)
    onError('')
    try {
      await deleteClinicSlot(classId, slotId)
      if (editingId === slotId) cancelEdit()
      await loadWeek()
    } catch (err) {
      onError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  // 학생 모드 — 슬롯 임시 토글 (서버 반영 X, 저장 버튼으로 일괄)
  function togglePendingSlot(slotId, slot) {
    setPendingSlotIds((prev) => {
      const next = new Set(prev)
      if (next.has(slotId)) {
        next.delete(slotId)
        return next
      }
      // 같은 요일·같은 시간 슬롯이 이미 선택돼 있다면 제거 (시간 중복 방지)
      const startTime = slot.startTime
      for (const row of weekView?.slots ?? []) {
        const s = row.slot
        if (
          next.has(s.slotId) &&
          s.dayOfWeek === slot.dayOfWeek &&
          s.startTime === startTime
        ) {
          next.delete(s.slotId)
        }
      }
      next.add(slotId)
      return next
    })
  }

  async function handleSaveReservations() {
    if (!weekView) return
    const mineNow = new Set(
      weekView.slots.filter((r) => r.myReservationId).map((r) => r.slot.slotId),
    )
    const toAdd = [...pendingSlotIds].filter((id) => !mineNow.has(id))
    const toRemove = [...mineNow].filter((id) => !pendingSlotIds.has(id))
    if (toAdd.length === 0 && toRemove.length === 0) return

    setSubmitting(true)
    onError('')
    try {
      for (const id of toRemove) {
        await cancelClinicReservation(classId, id)
      }
      for (const id of toAdd) {
        await reserveClinicSlot(classId, id)
      }
      await loadWeek()
    } catch (err) {
      onError(err.message)
      await loadWeek()
    } finally {
      setSubmitting(false)
    }
  }

  async function handleSaveResult(reservationId, payload) {
    setSubmitting(true)
    onError('')
    try {
      await updateClinicReservationResult(reservationId, payload)
      await loadWeek()
    } catch (err) {
      onError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  async function handleCreatePreset(payload) {
    setSubmitting(true)
    onError('')
    try {
      await createClinicPreset(classId, payload)
      setPresets(await fetchClinicPresets(classId))
    } catch (err) {
      onError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  async function handleUpdatePreset(presetId, payload) {
    setSubmitting(true)
    onError('')
    try {
      await updateClinicPreset(classId, presetId, payload)
      setPresets(await fetchClinicPresets(classId))
    } catch (err) {
      onError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  async function handleDeletePreset(presetId, name) {
    if (!window.confirm(`「${name}」 프리셋을 삭제할까요?`)) return
    setSubmitting(true)
    onError('')
    try {
      await deleteClinicPreset(classId, presetId)
      const next = await fetchClinicPresets(classId)
      setPresets(next)
      const defaultId = defaultPresetId(next)
      setForm((prev) => ({ ...prev, presetId: defaultId }))
    } catch (err) {
      onError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  const weekEnd = addDays(weekStart, 4)
  const bookingSlots = useMemo(() => {
    const slots = weekView?.slots ?? []
    if (!mySlotsOnly || !user?.userId) return slots
    return slots.filter((row) => row.slot.assistantId === user.userId)
  }, [weekView?.slots, mySlotsOnly, user?.userId])
  const isLocked = weekView?.weekStatus === 'LOCKED' || !weekView?.bookingOpen
  const statusLabel =
    weekView?.weekStatus === 'LOCKED'
      ? '잠금됨'
      : weekView?.bookingOpen
        ? '예약 가능'
        : '예약 마감'

  // 학생 화면: 요일별 그룹핑 (담임 모드는 기존 1행=1슬롯 유지)
  const slotsByDay = useMemo(() => {
    const map = {}
    for (const d of WEEKDAY_ORDER) map[d] = []
    for (const row of bookingSlots) {
      ;(map[row.slot.dayOfWeek] ||= []).push(row)
    }
    for (const d of Object.keys(map)) {
      map[d].sort((a, b) => (a.slot.startTime || '').localeCompare(b.slot.startTime || ''))
    }
    return map
  }, [bookingSlots])

  const mineNow = useMemo(
    () => new Set(bookingSlots.filter((r) => r.myReservationId).map((r) => r.slot.slotId)),
    [bookingSlots],
  )

  const hasReservationChanges = useMemo(() => {
    if (mineNow.size !== pendingSlotIds.size) return true
    for (const id of mineNow) if (!pendingSlotIds.has(id)) return true
    return false
  }, [mineNow, pendingSlotIds])

  if (loading) {
    return <p className="ams-class-detail__empty">불러오는 중…</p>
  }

  return (
    <section className="ams-class-detail__section">
      <h3 className="ams-class-detail__heading">클리닉</h3>
      <p className="ams-class-detail__hint-inline">
        {isStudent
          ? '담임이 슬롯을 설정하고, 학생이 슬롯을 예약합니다. (토 23:00 마감)'
          : '날짜를 눌러 해당 요일 슬롯·출결을 확인하세요. 카드의 「명」은 예약 학생 수, 「건」은 슬롯 수입니다.'}
      </p>

      <div className="ams-clinic-week">
        <label>
          주차 (월요일)
          <input
            type="date"
            value={weekStart}
            onChange={(e) => setWeekStart(e.target.value)}
          />
        </label>
        <span className="ams-clinic-week__range">
          {weekStart} ~ {weekEnd}
          <span
            className={
              isLocked
                ? 'ams-clinic-week__lock'
                : 'ams-pill ams-pill--primary-soft'
            }
          >
            {isLocked ? '🔒' : ''} {statusLabel}
          </span>
        </span>
        <button
          type="button"
          className="ams-btn ams-btn--ghost ams-btn--sm"
          onClick={() => setWeekStart(mondayOfWeek())}
        >
          이번 주
        </button>
      </div>

      {canManage && (
        <ClinicPresetSection
          presets={presets}
          canManage={canManage}
          submitting={submitting}
          onCreate={handleCreatePreset}
          onUpdate={handleUpdatePreset}
          onDelete={handleDeletePreset}
        />
      )}

      {canManage && (
        <form className="ams-clinic-form" onSubmit={handleCreate}>
          <label>
            요일
            <select
              value={form.dayOfWeek}
              onChange={(e) => setForm({ ...form, dayOfWeek: e.target.value })}
            >
              {CLINIC_DAY_OPTIONS.map((d) => (
                <option key={d.value} value={d.value}>
                  {d.label}
                </option>
              ))}
            </select>
          </label>
          <label>
            시작 시각
            <input
              type="time"
              value={form.startTime}
              onChange={(e) => setForm({ ...form, startTime: e.target.value })}
              required
            />
          </label>
          <label>
            조교
            <select
              value={form.assistantId}
              onChange={(e) => setForm({ ...form, assistantId: e.target.value })}
              required
            >
              <option value="" disabled>
                선택
              </option>
              {assistants.map((a) => (
                <option key={a.userId} value={a.userId}>
                  {a.name}
                </option>
              ))}
            </select>
          </label>
          <label>
            정원
            <input
              type="number"
              min={1}
              max={20}
              value={form.maxCapacity}
              onChange={(e) => setForm({ ...form, maxCapacity: e.target.value })}
            />
          </label>
          <ClinicPresetPicker
            presets={presets}
            value={form.presetId}
            onChange={(e) => setForm({ ...form, presetId: e.target.value })}
            disabled={submitting}
          />
          <StudentTargetPicker
            className="ams-assignment-form__full"
            classId={classId}
            allByDefault
            value={createTarget}
            onChange={setCreateTarget}
            disabled={submitting}
          />
          <button type="submit" className="ams-btn ams-btn--primary" disabled={submitting}>
            슬롯 추가
          </button>
        </form>
      )}

      {bookingSlots.length === 0 ? (
        <p className="ams-class-detail__empty">이 주에 등록된 클리닉 슬롯이 없습니다.</p>
      ) : !isStudent ? (
        <ClinicStaffCalendar
          weekStart={weekStart}
          bookingSlots={bookingSlots}
          selectedDay={selectedDay}
          onSelectDay={setSelectedDay}
          canManage={canManage}
          canViewResults={canViewResults}
          currentUserId={user?.userId}
          assistants={assistants}
          presets={presets}
          assistantLabel={assistantLabel}
          resultDraft={resultDraft}
          setResultDraft={setResultDraft}
          onSaveResult={handleSaveResult}
          submitting={submitting}
          editingId={editingId}
          editForm={editForm}
          setEditForm={setEditForm}
          onStartEdit={startEdit}
          onCancelEdit={cancelEdit}
          onUpdate={handleUpdate}
          onDelete={handleDelete}
        />
      ) : (
        <>
          <ul className="ams-clinic-day-list" aria-label="요일별 클리닉 슬롯">
            {WEEKDAY_ORDER.map((day) => {
              const rows = slotsByDay[day] || []
              if (rows.length === 0) return null
              const hasPendingInDay = rows.some((r) => pendingSlotIds.has(r.slot.slotId))
              const assistantNames = [
                ...new Set(
                  rows.map((r) => r.slot.assistantName || `#${r.slot.assistantId}`),
                ),
              ].join(' / ')

              return (
                <li
                  key={day}
                  className={
                    hasPendingInDay
                      ? 'ams-clinic-day-list__row ams-clinic-day-list__row--selected'
                      : 'ams-clinic-day-list__row'
                  }
                >
                  <span className="ams-clinic-day-list__day">{dayLabel(day)}</span>
                  <div className="ams-clinic-day-list__slots">
                    {rows.map((row) => {
                      const s = row.slot
                      const isPending = pendingSlotIds.has(s.slotId)
                      const time = s.startTime?.slice(0, 5) ?? ''
                      const disabled =
                        !weekView?.bookingOpen ||
                        submitting ||
                        (!isPending && row.full)
                      return (
                        <button
                          key={s.slotId}
                          type="button"
                          className={
                            isPending
                              ? 'ams-clinic-day-list__pill ams-clinic-day-list__pill--selected'
                              : 'ams-clinic-day-list__pill'
                          }
                          onClick={() => togglePendingSlot(s.slotId, s)}
                          disabled={disabled}
                          aria-pressed={isPending}
                          title={row.full && !isPending ? '정원 마감' : undefined}
                        >
                          {time}
                          {row.full && !isPending ? ' · 마감' : ''}
                        </button>
                      )
                    })}
                  </div>
                  {hasPendingInDay ? (
                    <span className="ams-clinic-day-list__selected">선택됨</span>
                  ) : (
                    <span className="ams-clinic-day-list__assistant">
                      {assistantNames}
                    </span>
                  )}
                </li>
              )
            })}
          </ul>

          {weekView?.bookingOpen && (
            <button
              type="button"
              className="ams-btn ams-btn--ghost ams-btn--block ams-clinic-save"
              onClick={handleSaveReservations}
              disabled={submitting || !hasReservationChanges}
            >
              {submitting
                ? '저장 중…'
                : hasReservationChanges
                  ? '예약 저장'
                  : '변경 사항 없음'}
            </button>
          )}
        </>
      )}
    </section>
  )
}
