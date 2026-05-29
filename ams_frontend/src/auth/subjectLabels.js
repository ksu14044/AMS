export const SUBJECT_OPTIONS = [
  { value: 'KO', label: '국어' },
  { value: 'EN', label: '영어' },
  { value: 'MATH', label: '수학' },
]

export function subjectLabel(subject) {
  return SUBJECT_OPTIONS.find((s) => s.value === subject)?.label ?? subject
}
