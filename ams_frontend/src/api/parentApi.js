import { apiRequest } from './client'

export function fetchParentChildren() {
  return apiRequest('/parent/children')
}

export function fetchChildPendingTasks(studentId) {
  return apiRequest(`/parent/children/${studentId}/pending-tasks`)
}

export function fetchChildStudyRecord(studentId, classId) {
  return apiRequest(`/parent/children/${studentId}/classes/${classId}/study-records`)
}

export function fetchChildReports(studentId) {
  return apiRequest(`/parent/children/${studentId}/reports`)
}

export function fetchParentLinksByStudent(studentId) {
  return apiRequest(`/parent-links/by-student/${studentId}`)
}

export function createParentLink(body) {
  return apiRequest('/parent-links', {
    method: 'POST',
    body: JSON.stringify(body),
  })
}

export function deleteParentLink(linkId) {
  return apiRequest(`/parent-links/${linkId}`, { method: 'DELETE' })
}
