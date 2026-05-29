export function studentRosterPathForRole(role) {
  if (role === 'ACADEMY_ADMIN') return '/admin/students'
  if (
    role === 'TEACHER_KO' ||
    role === 'TEACHER_EN' ||
    role === 'TEACHER_MATH' ||
    role === 'STAFF_OFFICE'
  ) {
    return '/teacher/students'
  }
  return null
}

export function studentRosterDescriptionForRole(role) {
  if (role === 'ACADEMY_ADMIN' || role === 'STAFF_OFFICE') {
    return '학원 전체 학생 명단입니다. 승인 대기 학생도 포함됩니다.'
  }
  if (role?.startsWith('TEACHER_')) {
    return '담임 반에 수강 배정된 학생 명단입니다.'
  }
  return ''
}
