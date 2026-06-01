import { apiRequest } from './client'

export function createSignupInvite({ kind, role }) {
  return apiRequest('/admin/signup-invites', {
    method: 'POST',
    body: JSON.stringify({ kind, role: role ?? null }),
  })
}

export function fetchPendingStaff() {
  return apiRequest('/admin/users/pending')
}

export function approveStaff(userId, { role, subject }) {
  return apiRequest(`/admin/users/${userId}/approve`, {
    method: 'POST',
    body: JSON.stringify({ role, subject: subject ?? null }),
  })
}

export function fetchClasses() {
  return apiRequest('/admin/classes')
}

export function createClass(payload) {
  return apiRequest('/admin/classes', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function updateClass(classId, payload) {
  return apiRequest(`/admin/classes/${classId}`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  })
}

export function fetchTeachers() {
  return apiRequest('/admin/teachers')
}

export function fetchStudents() {
  return apiRequest('/admin/students')
}

export function fetchEnrollments(classId) {
  return apiRequest(`/admin/classes/${classId}/enrollments`)
}

export function enrollStudent(classId, studentId, accessibleFrom) {
  return apiRequest(`/admin/classes/${classId}/enrollments`, {
    method: 'POST',
    body: JSON.stringify({ studentId, accessibleFrom: accessibleFrom || null }),
  })
}

export function unenrollStudent(enrollmentId) {
  return apiRequest(`/admin/enrollments/${enrollmentId}`, { method: 'DELETE' })
}
