export const STAFF_ROLES = [
  { value: 'TEACHER_KO', label: '국어 선생님', subject: 'KO' },
  { value: 'TEACHER_EN', label: '영어 선생님', subject: 'EN' },
  { value: 'TEACHER_MATH', label: '수학 선생님', subject: 'MATH' },
  { value: 'STAFF_OFFICE', label: '행정', subject: null },
  { value: 'ASSISTANT_KO', label: '국어 조교', subject: 'KO' },
  { value: 'ASSISTANT_EN', label: '영어 조교', subject: 'EN' },
  { value: 'ASSISTANT_MATH', label: '수학 조교', subject: 'MATH' },
]

export function roleLabel(role) {
  const found = STAFF_ROLES.find((r) => r.value === role)
  if (found) return found.label
  const map = {
    ACADEMY_ADMIN: '원장·관리자',
    STUDENT: '학생',
    PARENT: '학부모',
  }
  return map[role] || role
}

export function homePathForRole(role) {
  if (role === 'STUDENT') return '/student'
  if (role === 'PARENT') return '/parent'
  if (role === 'ACADEMY_ADMIN') return '/admin'
  if (role.startsWith('ASSISTANT_')) return '/assistant'
  return '/teacher'
}
