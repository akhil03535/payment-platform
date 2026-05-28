import apiClient from './apiClient'
import type { ApiResponse, AuthResponse, LoginRequest, RegisterRequest, User } from '../types'

export const authService = {
  async login(data: LoginRequest): Promise<AuthResponse> {
    const res = await apiClient.post<ApiResponse<AuthResponse>>('/api/auth/login', data)
    return res.data.data
  },

  async register(data: RegisterRequest): Promise<AuthResponse> {
    const res = await apiClient.post<ApiResponse<AuthResponse>>('/api/auth/register', data)
    return res.data.data
  },

  async getCurrentUser(): Promise<User> {
    const res = await apiClient.get<ApiResponse<User>>('/api/auth/me')
    return res.data.data
  },

  async logout(): Promise<void> {
    try {
      await apiClient.post('/api/auth/logout')
    } finally {
      localStorage.clear()
    }
  },

  async refreshToken(token: string): Promise<AuthResponse> {
    const res = await apiClient.post<ApiResponse<AuthResponse>>('/api/auth/refresh', null, {
      headers: { 'Refresh-Token': token },
    })
    return res.data.data
  },
}
