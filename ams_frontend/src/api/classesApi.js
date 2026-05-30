import { apiRequest } from './client'

/** 역할별 반 목록 (담임·학생·관리자 등) */
export function fetchClasses() {
  return apiRequest('/classes')
}

export function fetchClassDetail(classId) {
  return apiRequest(`/classes/${classId}`)
}

export function fetchLessonRecords(classId) {
  return apiRequest(`/classes/${classId}/lesson-records`)
}

export function fetchLessonRecord(classId, lessonRecordId) {
  return apiRequest(`/classes/${classId}/lesson-records/${lessonRecordId}`)
}

export function createLessonRecord(classId, payload) {
  return apiRequest(`/classes/${classId}/lesson-records`, {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function updateLessonRecord(classId, lessonRecordId, { summary }) {
  return apiRequest(`/classes/${classId}/lesson-records/${lessonRecordId}`, {
    method: 'PATCH',
    body: JSON.stringify({ summary }),
  })
}

export function fetchClassNotices(classId) {
  return apiRequest(`/classes/${classId}/notices`)
}

export function createClassNotice(classId, { title, body, attachmentUrl }) {
  return apiRequest(`/classes/${classId}/notices`, {
    method: 'POST',
    body: JSON.stringify({
      title,
      body,
      attachmentUrl: attachmentUrl || null,
    }),
  })
}

export function fetchClassSchedule(classId) {
  return apiRequest(`/classes/${classId}/schedule`)
}

export function updateClassSchedule(classId, slots) {
  return apiRequest(`/classes/${classId}/schedule`, {
    method: 'PATCH',
    body: JSON.stringify({ slots }),
  })
}

export function fetchClassTextbook(classId) {
  return apiRequest(`/classes/${classId}/textbook`)
}

export function updateClassTextbook(classId, payload) {
  return apiRequest(`/classes/${classId}/textbook`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  })
}

export function fetchClassVideos(classId) {
  return apiRequest(`/classes/${classId}/videos`)
}

export function createClassVideo(classId, payload) {
  return apiRequest(`/classes/${classId}/videos`, {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function updateClassVideo(classId, videoId, payload) {
  return apiRequest(`/classes/${classId}/videos/${videoId}`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  })
}

export function deleteClassVideo(classId, videoId) {
  return apiRequest(`/classes/${classId}/videos/${videoId}`, { method: 'DELETE' })
}

export function fetchClinicSlots(classId, weekStart) {
  return apiRequest(`/classes/${classId}/clinic/slots?weekStart=${weekStart}`)
}

export function fetchClinicWeek(classId, weekStart) {
  return apiRequest(`/classes/${classId}/clinic/weeks/${weekStart}`)
}

export function reserveClinicSlot(classId, slotId) {
  return apiRequest(`/classes/${classId}/clinic/reservations`, {
    method: 'PUT',
    body: JSON.stringify({ slotId }),
  })
}

export function cancelClinicReservation(classId, slotId) {
  return apiRequest(`/classes/${classId}/clinic/reservations/cancel`, {
    method: 'PUT',
    body: JSON.stringify({ slotId }),
  })
}

export function updateClinicReservationResult(reservationId, payload) {
  return apiRequest(`/clinic/reservations/${reservationId}/result`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  })
}

export function fetchClinicAssistants(classId) {
  return apiRequest(`/classes/${classId}/clinic/assistants`)
}

export function createClinicSlot(classId, payload) {
  return apiRequest(`/classes/${classId}/clinic/slots`, {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function updateClinicSlot(classId, slotId, payload) {
  return apiRequest(`/classes/${classId}/clinic/slots/${slotId}`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  })
}

export function deleteClinicSlot(classId, slotId) {
  return apiRequest(`/classes/${classId}/clinic/slots/${slotId}`, { method: 'DELETE' })
}

export function fetchClassAssistants(classId) {
  return apiRequest(`/classes/${classId}/assistants`)
}

export function updateClassAssistants(classId, assistantIds) {
  return apiRequest(`/classes/${classId}/assistants`, {
    method: 'PUT',
    body: JSON.stringify({ assistantIds }),
  })
}

export function fetchHomeworks(classId) {
  return apiRequest(`/classes/${classId}/homeworks`)
}

export function createHomework(classId, payload) {
  return apiRequest(`/classes/${classId}/homeworks`, {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function fetchHomeworkSubmissions(classId, homeworkId) {
  return apiRequest(`/classes/${classId}/homeworks/${homeworkId}/submissions`)
}

export function updateHomeworkSubmission(classId, homeworkId, studentId, payload) {
  return apiRequest(`/classes/${classId}/homeworks/${homeworkId}/submissions/${studentId}`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  })
}

export function completeHomework(classId, homeworkId) {
  return apiRequest(`/classes/${classId}/homeworks/${homeworkId}/complete`, { method: 'PATCH' })
}

export function fetchHomeworkAnswerKeys(classId, homeworkId) {
  return apiRequest(`/classes/${classId}/homeworks/${homeworkId}/answer-keys`)
}

export function saveHomeworkAnswerKeys(classId, homeworkId, payload) {
  return apiRequest(`/classes/${classId}/homeworks/${homeworkId}/answer-keys`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export function gradeHomeworkSubmission(classId, homeworkId, studentId, payload) {
  return apiRequest(`/classes/${classId}/homeworks/${homeworkId}/submissions/${studentId}/grade`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  })
}

export function fetchTests(classId) {
  return apiRequest(`/classes/${classId}/tests`)
}

export function createTest(classId, payload) {
  return apiRequest(`/classes/${classId}/tests`, {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function fetchTestScores(classId, testId) {
  return apiRequest(`/classes/${classId}/tests/${testId}/scores`)
}

export function saveTestScores(classId, testId, scores) {
  return apiRequest(`/classes/${classId}/tests/${testId}/scores`, {
    method: 'PATCH',
    body: JSON.stringify({ scores }),
  })
}

function toInstant(dateStr, timeStr) {
  if (!dateStr) return null
  const t = timeStr || '00:00'
  return new Date(`${dateStr}T${t}:00`).toISOString()
}

export { toInstant }
