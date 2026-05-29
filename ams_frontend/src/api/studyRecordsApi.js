import { apiRequest } from './client'

export function fetchMyStudyRecord(classId) {
  return apiRequest(`/classes/${classId}/study-records/me`)
}

export function fetchStudyRecordStudents(classId) {
  return apiRequest(`/classes/${classId}/study-records/students`)
}

export function fetchStudentStudyRecord(classId, studentId) {
  return apiRequest(`/classes/${classId}/study-records/students/${studentId}`)
}
