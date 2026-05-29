const ACCESS_KEY = 'ams_access_token'
const REFRESH_KEY = 'ams_refresh_token'
const USER_KEY = 'ams_user'

export function saveSession({ accessToken, refreshToken, user }) {
  localStorage.setItem(ACCESS_KEY, accessToken)
  localStorage.setItem(REFRESH_KEY, refreshToken)
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

export function clearSession() {
  localStorage.removeItem(ACCESS_KEY)
  localStorage.removeItem(REFRESH_KEY)
  localStorage.removeItem(USER_KEY)
}

export function getStoredUser() {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw)
  } catch {
    return null
  }
}

export function getRefreshToken() {
  return localStorage.getItem(REFRESH_KEY)
}
