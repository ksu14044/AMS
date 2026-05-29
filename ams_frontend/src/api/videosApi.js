import { fetchWithAuth } from './client'

const API_BASE = '/api/v1'

export async function fetchMyVideoCertification(videoId) {
  const response = await fetchWithAuth(`${API_BASE}/videos/${videoId}/certifications/me`)
  const body = await response.json().catch(() => ({}))
  if (!response.ok || body.success === false) {
    throw new Error(body.message || '요청 처리에 실패했습니다.')
  }
  return body.data
}

export async function uploadVideoCertification(videoId, file) {
  const formData = new FormData()
  formData.append('file', file)
  const response = await fetchWithAuth(`${API_BASE}/videos/${videoId}/certifications`, {
    method: 'POST',
    body: formData,
  })
  const body = await response.json().catch(() => ({}))
  if (!response.ok || body.success === false) {
    throw new Error(body.message || '업로드에 실패했습니다.')
  }
  return body.data
}

export function mediaUrl(path) {
  if (!path) return null
  if (path.startsWith('http')) return path
  return path
}
