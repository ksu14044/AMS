import { apiRequest } from './client'

export function fetchStudentRoster(q) {
  const query = q?.trim() ? `?q=${encodeURIComponent(q.trim())}` : ''
  return apiRequest(`/students/roster${query}`)
}
