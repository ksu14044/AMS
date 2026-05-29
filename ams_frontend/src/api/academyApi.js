import { apiRequest } from './client'

export function fetchAcademyNotices() {
  return apiRequest('/academy/notices')
}

export function createAcademyNotice({ title, body, attachmentUrl }) {
  return apiRequest('/academy/notices', {
    method: 'POST',
    body: JSON.stringify({
      title,
      body,
      attachmentUrl: attachmentUrl || null,
    }),
  })
}
