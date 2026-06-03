import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { apiRequest, refreshSession } from '../api/client'
import { clearSession, getRefreshToken, getStoredUser, saveSession } from './tokenStorage'
import { homePathForRole } from './roleLabels'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [sessionReady, setSessionReady] = useState(false)

  useEffect(() => {
    async function bootstrapSession() {
      const storedUser = getStoredUser()
      const refreshToken = getRefreshToken()

      if (!storedUser && !refreshToken) {
        setSessionReady(true)
        return
      }

      if (!refreshToken) {
        clearSession()
        setSessionReady(true)
        return
      }

      try {
        const data = await refreshSession()
        setUser(data.user)
      } catch {
        clearSession()
        setUser(null)
      } finally {
        setSessionReady(true)
      }
    }

    bootstrapSession()
  }, [])

  const applySession = useCallback((tokenResponse) => {
    saveSession(tokenResponse)
    setUser(tokenResponse.user)
    return tokenResponse.user
  }, [])

  const login = useCallback(async ({ email, password }) => {
    const data = await apiRequest('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    })
    if (data.tokens) {
      const user = applySession(data.tokens)
      return { needsAcademySelection: false, user }
    }
    return {
      needsAcademySelection: true,
      loginToken: data.loginToken,
      academies: data.academies,
    }
  }, [applySession])

  const completeLogin = useCallback(
    async ({ loginToken, userId }) => {
      const data = await apiRequest('/auth/login/select', {
        method: 'POST',
        body: JSON.stringify({ loginToken, userId }),
      })
      return applySession(data)
    },
    [applySession],
  )

  const signupAcademy = useCallback(async (payload) => {
    const data = await apiRequest('/auth/signup/academy', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
    return applySession(data)
  }, [applySession])

  const signupStaff = useCallback(async (payload) => {
    const data = await apiRequest('/auth/signup/staff', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
    return applySession(data)
  }, [applySession])

  const signupStudent = useCallback(async (payload) => {
    const data = await apiRequest('/auth/signup/student', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
    return applySession(data)
  }, [applySession])

  const signupParent = useCallback(async (payload) => {
    const data = await apiRequest('/auth/signup/parent', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
    return applySession(data)
  }, [applySession])

  const logout = useCallback(() => {
    clearSession()
    setUser(null)
  }, [])

  const homePath = user ? homePathForRole(user.role) : '/login'

  const value = useMemo(
    () => ({
      user,
      sessionReady,
      isAuthenticated: Boolean(user),
      login,
      completeLogin,
      signupAcademy,
      signupStaff,
      signupStudent,
      signupParent,
      logout,
      homePath,
    }),
    [
      user,
      sessionReady,
      login,
      completeLogin,
      signupAcademy,
      signupStaff,
      signupStudent,
      signupParent,
      logout,
      homePath,
    ],
  )

  if (!sessionReady) {
    return null
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
