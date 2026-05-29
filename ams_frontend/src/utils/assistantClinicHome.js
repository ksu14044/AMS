import { fetchMyAssistantClinicWeek } from '../api/meApi'
import { addDays } from './weekDate'
import { CLINIC_WEEKDAY_ORDER } from './clinicWeekCalendar'

function buildSummaries(myItems, weekStart) {
  myItems.sort((a, b) => {
    const dayDiff =
      CLINIC_WEEKDAY_ORDER.indexOf(a.slot.dayOfWeek) -
      CLINIC_WEEKDAY_ORDER.indexOf(b.slot.dayOfWeek)
    if (dayDiff !== 0) return dayDiff
    const timeDiff = (a.slot.startTime || '').localeCompare(b.slot.startTime || '')
    if (timeDiff !== 0) return timeDiff
    return a.className.localeCompare(b.className)
  })

  const summaries = CLINIC_WEEKDAY_ORDER.map((day, index) => {
    const items = myItems.filter((item) => item.slot.dayOfWeek === day)
    const studentCount = items.reduce((sum, item) => sum + (item.reservations?.length ?? 0), 0)
    return {
      day,
      date: addDays(weekStart, index),
      slotCount: items.length,
      studentCount,
      items,
    }
  })

  return { weekStart, summaries, totalSlots: myItems.length }
}

export async function loadMyClinicWeek(_assistantUserId, weekStart) {
  const data = await fetchMyAssistantClinicWeek(weekStart)
  const myItems = (data?.slots ?? []).map((row) => ({
    classId: row.classId,
    className: row.className,
    slot: row.slot,
    bookedCount: row.bookedCount,
    maxCapacity: row.maxCapacity,
    full: row.full,
    reservations: row.reservations ?? [],
  }))
  return buildSummaries(myItems, data.weekStart ?? weekStart)
}

export function pickDefaultAssistantClinicDay(summaries) {
  const withSlots = summaries.find((s) => s.slotCount > 0)
  return withSlots?.day ?? 'MON'
}
