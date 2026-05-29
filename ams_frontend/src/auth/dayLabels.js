export const DAY_OPTIONS = [
  { value: 'MON', label: '월' },
  { value: 'TUE', label: '화' },
  { value: 'WED', label: '수' },
  { value: 'THU', label: '목' },
  { value: 'FRI', label: '금' },
  { value: 'SAT', label: '토' },
  { value: 'SUN', label: '일' },
]

export const CLINIC_DAY_OPTIONS = DAY_OPTIONS.filter((d) =>
  ['MON', 'TUE', 'WED', 'THU', 'FRI'].includes(d.value),
)

export function dayLabel(day) {
  return DAY_OPTIONS.find((d) => d.value === day)?.label ?? day
}
