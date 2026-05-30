/** @typedef {{ mode: 'all' | 'custom', studentIds: number[] }} TargetPickerValue */

export const ALL_CLASS_TARGET = { mode: 'all', studentIds: [] }

export const EMPTY_TARGET = { mode: 'custom', studentIds: [] }

export function createInitialTarget(allByDefault) {
  return allByDefault ? { ...ALL_CLASS_TARGET } : { ...EMPTY_TARGET }
}

/** API payload: undefined = 기본(숙제·테스트·클리닉=반 전원), 배열 = 명시 대상 */
export function buildTargetStudentIdsPayload(target, allByDefault) {
  if (allByDefault && target.mode === 'all') return undefined
  return target.studentIds
}

/** API 응답 targets → StudentTargetPicker 값 */
export function targetFromResponse(targets, allByDefault) {
  if (!targets) return createInitialTarget(allByDefault)
  if (targets.allClassTargeted) return { ...ALL_CLASS_TARGET }
  return { mode: 'custom', studentIds: [...(targets.targetStudentIds ?? [])] }
}

export function formatTargetSummary(targets) {
  if (!targets) return '—'
  if (targets.allClassTargeted) return '반 전원'
  const count = targets.targetStudentIds?.length ?? 0
  return count > 0 ? `${count}명` : '대상 없음'
}

export function formatVideoCertTargetSummary(targets) {
  if (!targets) return '—'
  const count = targets.targetStudentIds?.length ?? 0
  return count > 0 ? `인증 ${count}명` : '인증 없음'
}

export function requiresVideoCertification(video, studentId) {
  if (!studentId) return false
  return (video?.targets?.targetStudentIds ?? []).includes(studentId)
}
