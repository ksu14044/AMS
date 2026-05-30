import { addDays } from './weekDate'

export const CLINIC_WEEKDAY_ORDER = ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN']

export function summarizeClinicWeekByDay(bookingSlots, weekStart) {
  return CLINIC_WEEKDAY_ORDER.map((day, index) => {
    const rows = bookingSlots
      .filter((r) => r.slot.dayOfWeek === day)
      .sort((a, b) => (a.slot.startTime || '').localeCompare(b.slot.startTime || ''))
    const slotCount = rows.length
    const studentCount = rows.reduce(
      (sum, r) => sum + (r.reservations?.length ?? r.bookedCount ?? 0),
      0,
    )
    return {
      day,
      date: addDays(weekStart, index),
      slotCount,
      studentCount,
      rows,
    }
  })
}

export function pickDefaultClinicDay(summaries) {
  const withSlots = summaries.find((d) => d.slotCount > 0)
  return withSlots?.day ?? 'MON'
}
