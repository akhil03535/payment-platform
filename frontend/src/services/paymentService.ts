import apiClient from './apiClient'
import type {
  ApiResponse, PageResponse,
  Payment, CreatePaymentRequest,
  Analytics, Transaction,
} from '../types'

export const paymentService = {
  async createPayment(
    data: CreatePaymentRequest,
    idempotencyKey?: string,
  ): Promise<Payment> {
    const headers: Record<string, string> = {}
    if (idempotencyKey) headers['Idempotency-Key'] = idempotencyKey

    const res = await apiClient.post<ApiResponse<Payment>>('/payments', data, { headers })
    return res.data.data
  },

  async getPayment(id: string): Promise<Payment> {
    const res = await apiClient.get<ApiResponse<Payment>>(`/payments/${id}`)
    return res.data.data
  },

  async getPayments(params?: {
    page?: number
    size?: number
    status?: string
  }): Promise<PageResponse<Payment>> {
    const res = await apiClient.get<ApiResponse<PageResponse<Payment>>>('/payments', { params })
    return res.data.data
  },

  async retryPayment(id: string): Promise<Payment> {
    const res = await apiClient.post<ApiResponse<Payment>>(`/payments/${id}/retry`)
    return res.data.data
  },

  async reversePayment(id: string): Promise<Payment> {
    const res = await apiClient.post<ApiResponse<Payment>>(`/payments/${id}/reverse`)
    return res.data.data
  },

  async getAnalytics(days = 30): Promise<Analytics> {
    const res = await apiClient.get<ApiResponse<Analytics>>('/payments/analytics', {
      params: { days },
    })
    return res.data.data
  },
}

export const transactionService = {
  async getTransactions(params?: {
    page?: number
    size?: number
  }): Promise<PageResponse<Transaction>> {
    const res = await apiClient.get<ApiResponse<PageResponse<Transaction>>>(
      '/transactions', { params },
    )
    return res.data.data
  },

  async getTransaction(id: string): Promise<Transaction> {
    const res = await apiClient.get<ApiResponse<Transaction>>(`/transactions/${id}`)
    return res.data.data
  },
}
