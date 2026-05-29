import { clearSession, getRefreshToken, saveSession } from '../auth/tokenStorage'

const API_BASE = '/api/v1'

const AUTH_SKIP_REFRESH_PREFIXES = [
  '/auth/login',
  '/auth/refresh',
  '/auth/signup/',
  '/auth/signup-invites/',
]

function shouldSkipRefresh(path) {
  return AUTH_SKIP_REFRESH_PREFIXES.some((prefix) => path.startsWith(prefix))
}

let refreshInFlight = null

export async function refreshSession() {
  const refreshToken = getRefreshToken()
  if (!refreshToken) {
    const error = new Error('로그인이 필요합니다.')
    error.status = 401
    throw error
  }

  const response = await fetch(`${API_BASE}/auth/refresh`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken }),
  })

  const body = await response.json().catch(() => ({}))
  if (!response.ok || body.success === false) {
    const error = new Error(body.message || '로그인이 만료되었습니다.')
    error.status = response.status
    throw error
  }

  saveSession(body.data)
  return body.data
}

function refreshAccessToken() {
  if (!refreshInFlight) {
    refreshInFlight = refreshSession().finally(() => {
      refreshInFlight = null
    })
  }
  return refreshInFlight
}

function redirectToLogin() {
  const path = window.location.pathname
  if (path.startsWith('/login') || path.startsWith('/signup')) {
    return
  }
  window.location.assign('/login')
}

function handleAuthFailure() {
  clearSession()
  redirectToLogin()
}

export async function fetchWithAuth(url, options = {}, retried = false) {
  const headers = new Headers(options.headers || {})
  const accessToken = localStorage.getItem('ams_access_token')
  if (accessToken) {
    headers.set('Authorization', `Bearer ${accessToken}`)
  }

  const response = await fetch(url, { ...options, headers })

  if (response.status === 401 && !retried) {
    try {
      await refreshAccessToken()
      return fetchWithAuth(url, options, true)
    } catch {
      handleAuthFailure()
      throw new Error('로그인이 만료되었습니다. 다시 로그인해 주세요.')
    }
  }

  return response
}

export async function apiRequest(path, options = {}, retried = false) {
  const headers = {
    'Content-Type': 'application/json',
    ...options.headers,
  }

  const accessToken = localStorage.getItem('ams_access_token')
  if (accessToken) {
    headers.Authorization = `Bearer ${accessToken}`
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers,
  })

  if (response.status === 401 && !retried && !shouldSkipRefresh(path)) {
    try {
      await refreshAccessToken()
      return apiRequest(path, options, true)
    } catch {
      handleAuthFailure()
      throw new Error('로그인이 만료되었습니다. 다시 로그인해 주세요.')
    }
  }

  const body = await response.json().catch(() => ({}))

  if (!response.ok || body.success === false) {
    const message = body.message || '요청 처리에 실패했습니다.'
    const error = new Error(message)
    error.code = body.code
    error.status = response.status
    throw error
  }

  return body.data
}
