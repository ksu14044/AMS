import { useCallback, useEffect, useState } from 'react'
import { dayLabel } from '../auth/dayLabels'
import { loadMyClinicWeek, pickDefaultAssistantClinicDay } from '../utils/assistantClinicHome'
import { mondayOfWeek } from '../utils/weekDate'
import AssistantClinicSlotExpand from './AssistantClinicSlotExpand'
import DashboardCard from './DashboardCard'

function formatDayNumber(isoDate) {
  const d = new Date(`${isoDate}T12:00:00`)
  return d.getDate()
}

function formatMonthDay(isoDate) {
  const d = new Date(`${isoDate}T12:00:00`)
  return `${d.getMonth() + 1}월 ${d.getDate()}일`
}

export default function AssistantClinicWeekCard({ userId }) {
  const [weekStart] = useState(() => mondayOfWeek())
  const [summaries, setSummaries] = useState([])
  const [totalSlots, setTotalSlots] = useState(0)
  const [selectedDay, setSelectedDay] = useState('MON')
  const [expandedSlotId, setExpandedSlotId] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    if (!userId) return
    setLoading(true)
    setError('')
    try {
      const data = await loadMyClinicWeek(userId, weekStart)
      setSummaries(data.summaries)
      setTotalSlots(data.totalSlots)
      setSelectedDay((prev) => {
        if (data.summaries.some((s) => s.day === prev && s.slotCount > 0)) return prev
        return pickDefaultAssistantClinicDay(data.summaries)
      })
    } catch (err) {
      setError(err.message)
      setSummaries([])
      setTotalSlots(0)
    } finally {
      setLoading(false)
    }
  }, [userId, weekStart])

  useEffect(() => {
    load()
  }, [load])

  const activeSummary = summaries.find((s) => s.day === selectedDay)

  function toggleSlot(slotId) {
    setExpandedSlotId((prev) => (prev === slotId ? null : slotId))
    setError('')
  }

  function handleDayChange(day) {
    setSelectedDay(day)
    setExpandedSlotId(null)
    setError('')
  }

  return (
    <DashboardCard title="내 이번 주 클리닉">
      {loading ? (
        <p className="ams-class-home__empty">불러오는 중…</p>
      ) : error && totalSlots === 0 ? (
        <p className="ams-class-home__error">{error}</p>
      ) : totalSlots === 0 ? (
        <p className="ams-class-home__empty">이번 주에 배정된 클리닉 슬롯이 없습니다.</p>
      ) : (
        <div className="ams-clinic-calendar ams-clinic-calendar--home">
          <p className="ams-assistant-clinic-home__range">
            {formatMonthDay(weekStart)} 주 · 내 슬롯 {totalSlots}건
          </p>

          <div className="ams-clinic-calendar__grid" role="tablist" aria-label="내 클리닉 주간 달력">
            {summaries.map((summary) => {
              const isSelected = summary.day === selectedDay
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
                  onClick={() => handleDayChange(summary.day)}
                >
                  <span className="ams-clinic-calendar__weekday">{dayLabel(summary.day)}</span>
                  <span className="ams-clinic-calendar__date-num">
                    {formatDayNumber(summary.date)}
                  </span>
                  <span className="ams-clinic-calendar__stats">
                    <span>{summary.studentCount}명</span>
                    <span className="ams-clinic-calendar__dot">·</span>
                    <span>{summary.slotCount}건</span>
                  </span>
                </button>
              )
            })}
          </div>

          {activeSummary && (
            <div className="ams-assistant-clinic-home__day">
              <p className="ams-assistant-clinic-home__day-title">
                {dayLabel(activeSummary.day)} ({formatMonthDay(activeSummary.date)})
                {activeSummary.slotCount > 0
                  ? ` · ${activeSummary.studentCount}명 · ${activeSummary.slotCount}건`
                  : ''}
              </p>
              <p className="ams-assistant-clinic-home__hint">슬롯을 눌러 출결을 입력하세요.</p>

              {error && <p className="ams-class-home__error">{error}</p>}

              {activeSummary.slotCount === 0 ? (
                <p className="ams-class-home__empty">이 날짜에 배정된 클리닉이 없습니다.</p>
              ) : (
                <ul className="ams-assistant-clinic-home__list">
                  {activeSummary.items.map((item) => {
                    const slotId = item.slot.slotId
                    const isExpanded = expandedSlotId === slotId
                    const time = item.slot.startTime?.slice(0, 5) ?? ''
                    const studentCount = item.reservations?.length ?? 0
                    const names = item.reservations?.map((r) => r.studentName).join(', ')
                    return (
                      <li
                        key={slotId}
                        className={
                          isExpanded
                            ? 'ams-assistant-clinic-home__item ams-assistant-clinic-home__item--expanded'
                            : 'ams-assistant-clinic-home__item'
                        }
                      >
                        <button
                          type="button"
                          className="ams-assistant-clinic-home__link"
                          aria-expanded={isExpanded}
                          onClick={() => toggleSlot(slotId)}
                        >
                          <span className="ams-assistant-clinic-home__time">{time}</span>
                          <span className="ams-assistant-clinic-home__class">{item.className}</span>
                          <span className="ams-assistant-clinic-home__meta">
                            {studentCount > 0
                              ? `${studentCount}명${names ? ` · ${names}` : ''}`
                              : '예약 없음'}
                          </span>
                        </button>
                        {isExpanded && (
                          <AssistantClinicSlotExpand
                            item={item}
                            onSaved={load}
                            onError={setError}
                          />
                        )}
                      </li>
                    )
                  })}
                </ul>
              )}
            </div>
          )}
        </div>
      )}
    </DashboardCard>
  )
}
