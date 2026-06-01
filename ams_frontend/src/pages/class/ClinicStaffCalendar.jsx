import { useMemo } from 'react'
import { dayLabel } from '../../auth/dayLabels'
import {
  pickDefaultClinicDay,
  summarizeClinicWeekByDay,
} from '../../utils/clinicWeekCalendar'
import ClinicDayDetailPanel from './ClinicDayDetailPanel'

function formatDayNumber(isoDate) {
  const d = new Date(`${isoDate}T12:00:00`)
  return d.getDate()
}

function formatMonthDay(isoDate) {
  const d = new Date(`${isoDate}T12:00:00`)
  return `${d.getMonth() + 1}월 ${d.getDate()}일`
}

export default function ClinicStaffCalendar({
  weekStart,
  bookingSlots,
  selectedDay,
  onSelectDay,
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
  const summaries = useMemo(
    () => summarizeClinicWeekByDay(bookingSlots, weekStart),
    [bookingSlots, weekStart],
  )

  const activeDay = selectedDay ?? pickDefaultClinicDay(summaries)
  const activeSummary = summaries.find((d) => d.day === activeDay)

  return (
    <div className="ams-clinic-calendar">
      <div className="ams-clinic-calendar__grid" role="tablist" aria-label="주간 클리닉 달력">
        {summaries.map((summary) => {
          const isSelected = summary.day === activeDay
          const hasSlots = summary.slotCount > 0
          return (
            <button
              key={summary.day}
              type="button"
              role="tab"
              aria-selected={isSelected}
              className={
                isSelected
                  ? 'ams-clinic-calendar__day ams-clinic-calendar__day--selected'
                  : hasSlots
                    ? 'ams-clinic-calendar__day ams-clinic-calendar__day--has-slots'
                    : 'ams-clinic-calendar__day'
              }
              onClick={() => onSelectDay(summary.day)}
            >
              <span className="ams-clinic-calendar__weekday">{dayLabel(summary.day)}</span>
              <span className="ams-clinic-calendar__date-num">{formatDayNumber(summary.date)}</span>
              <span className="ams-clinic-calendar__stats">
                <span>{summary.studentCount}명</span>
                <span className="ams-clinic-calendar__dot">·</span>
                <span>{summary.slotCount}건</span>
              </span>
            </button>
          )
        })}
      </div>

      <ClinicDayDetailPanel
        daySummary={activeSummary}
        dayLabel={`${dayLabel(activeDay)} (${formatMonthDay(activeSummary?.date)})`}
        canManage={canManage}
        canViewResults={canViewResults}
        currentUserId={currentUserId}
        assistants={assistants}
        presets={presets}
        assistantLabel={assistantLabel}
        resultDraft={resultDraft}
        setResultDraft={setResultDraft}
        onSaveResult={onSaveResult}
        submitting={submitting}
        editingId={editingId}
        editForm={editForm}
        setEditForm={setEditForm}
        onStartEdit={onStartEdit}
        onCancelEdit={onCancelEdit}
        onUpdate={onUpdate}
        onDelete={onDelete}
      />
    </div>
  )
}
