import { createContext, useContext, useEffect, useMemo, useState } from 'react'
import { authApi } from '../api/client'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem('settlehub_token'))
  const [user, setUser] = useState(() => {
    const raw = localStorage.getItem('settlehub_user')
    return raw ? JSON.parse(raw) : null
  })
  const [loading, setLoading] = useState(Boolean(token))

  useEffect(() => {
    if (!token) {
      setLoading(false)
      return
    }
    authApi
      .me()
      .then((me) => {
        setUser(me)
        localStorage.setItem('settlehub_user', JSON.stringify(me))
      })
      .catch(() => {
        localStorage.removeItem('settlehub_token')
        localStorage.removeItem('settlehub_user')
        setToken(null)
        setUser(null)
      })
      .finally(() => setLoading(false))
  }, [token])

  const value = useMemo(
    () => ({
      token,
      user,
      loading,
      async login(email, password) {
        const data = await authApi.login(email, password)
        localStorage.setItem('settlehub_token', data.accessToken)
        localStorage.setItem('settlehub_user', JSON.stringify(data.user))
        setToken(data.accessToken)
        setUser(data.user)
        return data.user
      },
      logout() {
        localStorage.removeItem('settlehub_token')
        localStorage.removeItem('settlehub_user')
        setToken(null)
        setUser(null)
      },
      hasRole(...roles) {
        return user && roles.includes(user.role)
      },
    }),
    [token, user, loading],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
