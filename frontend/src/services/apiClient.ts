import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios'

const DEFAULT_API_ORIGIN = 'https://payment-platform-backend.onrender.com'
const rawApiUrl = (import.meta.env.VITE_API_URL as string | undefined)?.trim()
const normalizedApiOrigin = (rawApiUrl || DEFAULT_API_ORIGIN)
  .replace(/\/+$/, '')
  .replace(/\/api$/, '')

const apiOrigin = /^https?:\/\//i.test(normalizedApiOrigin)
  ? normalizedApiOrigin
  : DEFAULT_API_ORIGIN

const BASE_URL = `${apiOrigin}/api`

export const apiClient = axios.create({
  baseURL: BASE_URL,
  timeout: 15_000,
  headers: { 'Content-Type': 'application/json' },
})

// ── Request interceptor ───────────────────────────────
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('accessToken')
    if (token) config.headers.Authorization = `Bearer ${token}`

    // Correlation ID for tracing
    config.headers['X-Correlation-ID'] =
      Math.random().toString(36).substring(2, 10).toUpperCase()

    return config
  },
  (error) => Promise.reject(error),
)

// ── Response interceptor ─────────────────────────────
let isRefreshing = false
let failedQueue: Array<{ resolve: (v: string) => void; reject: (e: unknown) => void }> = []

const processQueue = (error: unknown, token: string | null = null) => {
  failedQueue.forEach(({ resolve, reject }) => {
    error ? reject(error) : resolve(token!)
  })
  failedQueue = []
}

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const original = error.config as InternalAxiosRequestConfig & { _retry?: boolean }

    if (error.response?.status === 401 && !original._retry) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject })
        }).then((token) => {
          original.headers.Authorization = `Bearer ${token}`
          return apiClient(original)
        })
      }

      original._retry = true
      isRefreshing = true

      const refreshToken = localStorage.getItem('refreshToken')
      if (!refreshToken) {
        isRefreshing = false
        localStorage.clear()
        window.location.href = '/login'
        return Promise.reject(error)
      }

      try {
        const { data } = await axios.post(`${BASE_URL}/auth/refresh`, null, {
          headers: { 'Refresh-Token': refreshToken },
        })
        const newToken = data.data.accessToken
        localStorage.setItem('accessToken', newToken)
        localStorage.setItem('refreshToken', data.data.refreshToken)
        apiClient.defaults.headers.common.Authorization = `Bearer ${newToken}`
        processQueue(null, newToken)
        original.headers.Authorization = `Bearer ${newToken}`
        return apiClient(original)
      } catch (refreshError) {
        processQueue(refreshError, null)
        localStorage.clear()
        window.location.href = '/login'
        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    }

    return Promise.reject(error)
  },
)

export default apiClient
