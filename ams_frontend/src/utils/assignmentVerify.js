export const ASSIGNMENT_STATUS_LABEL = { SCHEDULED: '예정', COMPLETED: '완료' }

function sortByPendingThenDate(items, dateKey) {
  return [...items].sort((a, b) => {
    const aPending = (a.pendingGradeCount ?? 0) > 0 ? 1 : 0
    const bPending = (b.pendingGradeCount ?? 0) > 0 ? 1 : 0
    if (aPending !== bPending) return bPending - aPending
    return new Date(b[dateKey]) - new Date(a[dateKey])
  })
}

export function toHomeworkBoardItems(homeworks, { forStudent = false } = {}) {
  return sortByPendingThenDate(homeworks, 'createdAt').map((h) => ({
    id: String(h.homeworkId),
    title: h.title,
    status: h.status,
    statusLabel: ASSIGNMENT_STATUS_LABEL[h.status] || h.status,
    subtitle: h.createdAt
      ? new Date(h.createdAt).toLocaleDateString('ko-KR', {
          year: 'numeric',
          month: 'short',
          day: 'numeric',
        })
      : undefined,
    chips: [
      !forStudent && (h.pendingGradeCount ?? 0) > 0 ? `미입력 ${h.pendingGradeCount}명` : null,
      h.questionCount ? `${h.questionCount}문항` : null,
    ].filter(Boolean),
  }))
}

export function toTestBoardItems(tests, { forStudent = false } = {}) {
  return sortByPendingThenDate(tests, 'testAt').map((t) => ({
    id: String(t.testId),
    title: t.title,
    status: t.status,
    statusLabel: ASSIGNMENT_STATUS_LABEL[t.status] || t.status,
    subtitle: new Date(t.testAt).toLocaleString('ko-KR', {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    }),
    chips: [
      !forStudent && (t.pendingGradeCount ?? 0) > 0 ? `미입력 ${t.pendingGradeCount}명` : null,
      t.questionCount ? `${t.questionCount}문항` : null,
      t.retakeAttemptNo > 0 ? `재시험 ${t.retakeAttemptNo}회` : null,
      t.countOnlyGrading === false ? '중간·기말' : null,
      t.classAverage != null ? `반평균 ${t.classAverage}%` : null,
    ].filter(Boolean),
  }))
}
