import { ClinicPresetPicker } from '../../components/ClinicPresetSection'
import ClinicSlotResultTable from '../../components/ClinicSlotResultTable'

function canEditSlotResult(canManage, canViewResults, currentUserId, assistantId) {
  if (!canViewResults) return false
  if (canManage) return true
  return assistantId != null && assistantId === currentUserId
}

export default function ClinicDayDetailPanel({
  daySummary,
  dayLabel,
  canManage,
  canViewResults,
  currentUserId,
  assistants,
  presets,
  assistantLabel,
  resultDraft,
  setResultDraft,
  onSaveResult,
  submitting,
  editingId,
  editForm,
  setEditForm,
  onStartEdit,
  onCancelEdit,
  onUpdate,
  onDelete,
}) {
  if (!daySummary) return null

  const { rows, slotCount, studentCount } = daySummary

  if (slotCount === 0) {
    return (
      <section className="ams-clinic-day-panel">
        <h4 className="ams-clinic-day-panel__title">{dayLabel} 상세</h4>
        <p className="ams-class-detail__empty">이 날짜에 등록된 클리닉 슬롯이 없습니다.</p>
      </section>
    )
  }

  return (
    <section className="ams-clinic-day-panel" aria-label={`${dayLabel} 클리닉 출결`}>
      <header className="ams-clinic-day-panel__head">
        <h4 className="ams-clinic-day-panel__title">{dayLabel} 출결</h4>
        <p className="ams-clinic-day-panel__meta">
          슬롯 {slotCount}건 · 예약 {studentCount}명
        </p>
      </header>

      <ul className="ams-clinic-day-panel__slots">
        {rows.map((row) => {
          const s = row.slot
          const fields = s.resultFields ?? []
          const isEditing = editingId === s.slotId && canManage
          const editableResult = canEditSlotResult(
            canManage,
            canViewResults,
            currentUserId,
            s.assistantId,
          )
          const time = s.startTime?.slice(0, 5) ?? ''

          if (isEditing) {
            return (
              <li key={s.slotId} className="ams-clinic-day-panel__slot">
                <form className="ams-clinic-form ams-clinic-form--inline" onSubmit={onUpdate}>
                  <label>
                    시각
                    <input
                      type="time"
                      value={editForm.startTime}
                      onChange={(e) => setEditForm({ ...editForm, startTime: e.target.value })}
                      required
                    />
                  </label>
                  <label>
                    조교
                    <select
                      value={editForm.assistantId}
                      onChange={(e) => setEditForm({ ...editForm, assistantId: e.target.value })}
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
                      value={editForm.maxCapacity}
                      onChange={(e) => setEditForm({ ...editForm, maxCapacity: e.target.value })}
                    />
                  </label>
                  <ClinicPresetPicker
                    presets={presets}
                    value={editForm.presetId}
                    onChange={(e) => setEditForm({ ...editForm, presetId: e.target.value })}
                    disabled={submitting}
                  />
                  <div className="ams-clinic-list__actions">
                    <button type="submit" className="ams-btn ams-btn--primary" disabled={submitting}>
                      저장
                    </button>
                    <button type="button" className="ams-btn ams-btn--ghost" onClick={onCancelEdit}>
                      취소
                    </button>
                  </div>
                </form>
              </li>
            )
          }

          return (
            <li key={s.slotId} className="ams-clinic-day-panel__slot">
              <div className="ams-clinic-day-panel__slot-head">
                <strong className="ams-clinic-day-panel__time">{time}</strong>
                <span className="ams-clinic-day-panel__assistant">
                  {assistantLabel(s.assistantId, s.assistantName)}
                </span>
                {s.presetName && (
                  <span className="ams-clinic-day-panel__preset">{s.presetName}</span>
                )}
                <span
                  className={
                    row.full
                      ? 'ams-clinic-day-panel__capacity ams-clinic-day-panel__capacity--full'
                      : 'ams-clinic-day-panel__capacity'
                  }
                >
                  {row.bookedCount}/{row.maxCapacity}
                </span>
              </div>

              {row.reservations?.length > 0 ? (
                <ClinicSlotResultTable
                  fields={fields}
                  reservations={row.reservations}
                  resultDraft={resultDraft}
                  onDraftChange={(reservationId, next) =>
                    setResultDraft((prev) => ({ ...prev, [reservationId]: next }))
                  }
                  onSaveResult={onSaveResult}
                  submitting={submitting}
                  editable={editableResult}
                />
              ) : (
                <p className="ams-clinic-day-panel__no-student">예약 없음</p>
              )}

              {canManage && (
                <div className="ams-clinic-day-panel__actions">
                  <button
                    type="button"
                    className="ams-btn ams-btn--ghost ams-btn--sm"
                    disabled={submitting}
                    onClick={() => onStartEdit(s)}
                  >
                    슬롯 수정
                  </button>
                  <button
                    type="button"
                    className="ams-btn ams-btn--ghost ams-btn--sm"
                    disabled={submitting}
                    onClick={() => onDelete(s.slotId)}
                  >
                    삭제
                  </button>
                </div>
              )}
            </li>
          )
        })}
      </ul>
    </section>
  )
}
