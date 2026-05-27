import React, { createContext, useContext, useEffect, useState, useCallback } from 'react'
import type { User, LoginRequest, RegisterRequest } from '../types'
import { authService } from '../services/authService'
import toast from 'react-hot-toast'

interface AuthContextValue {
  user: User | null
  isAuthenticated: boolean
  isLoading: boolean
  login: (data: LoginRequest) => Promise<void>
  register: (data: RegisterRequest) => Promise<void>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  // Restore session on mount
  useEffect(() => {
    const token = localStorage.getItem('accessToken')
    if (!token) { setIsLoading(false); return }

    authService.getCurrentUser()
      .then(setUser)
      .catch(() => localStorage.clear())
      .finally(() => setIsLoading(false))
  }, [])

  const login = useCallback(async (data: LoginRequest) => {
    const res = await authService.login(data)
    localStorage.setItem('accessToken', res.accessToken)
    localStorage.setItem('refreshToken', res.refreshToken)
    setUser(res.user)
  }, [])

  const register = useCallback(async (data: RegisterRequest) => {
    const res = await authService.register(data)
    localStorage.setItem('accessToken', res.accessToken)
    localStorage.setItem('refreshToken', res.refreshToken)
    setUser(res.user)
  }, [])

  const logout = useCallback(async () => {
    try {
      await authService.logout()
    } catch {
      // ignore
    } finally {
      localStorage.clear()
      setUser(null)
      toast.success('Logged out successfully')
    }
  }, [])

  return (
    <AuthContext.Provider value={{
      user,
      isAuthenticated: !!user,
      isLoading,
      login,
      register,
      logout,
    }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within <AuthProvider>')
  return ctx
}
