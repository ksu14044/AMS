const API_BASE = '/api/v1'

export async function fetchSignupInvite(token) {
  const response = await fetch(`${API_BASE}/auth/signup/invite?token=${encodeURIComponent(token)}`)
  const body = await response.json().catch(() => ({}))
  if (!response.ok || body.success === false) {
    throw new Error(body.message || '가입 링크를 확인할 수 없습니다.')
  }
  return body.data
}
