export const ASSIGNMENT_STATUS_LABEL = { SCHEDULED: '예정', COMPLETED: '완료' }

export function toHomeworkBoardItems(homeworks) {
  return [...homeworks]
    .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
    .map((h) => ({
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
      chips: [h.questionCount ? `${h.questionCount}문항` : null].filter(Boolean),
    }))
}

export function toTestBoardItems(tests) {
  return [...tests]
    .sort((a, b) => new Date(b.testAt) - new Date(a.testAt))
    .map((t) => ({
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
        t.questionCount ? `${t.questionCount}문항` : null,
        t.retakeAttemptNo > 0 ? `재시험 ${t.retakeAttemptNo}회` : null,
        t.classAverage != null ? `반평균 ${t.classAverage}%` : null,
      ].filter(Boolean),
    }))
}
