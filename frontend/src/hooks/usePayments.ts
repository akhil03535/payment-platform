import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { paymentService, transactionService } from '../services/paymentService'
import type {
  Analytics,
  PageResponse,
  Payment,
  Transaction,
} from '../types'
import toast from 'react-hot-toast'

// ── Keys ──────────────────────────────────────────────
export const queryKeys = {
  payments:     (p?: object) => ['payments', p] as const,
  payment:      (id: string) => ['payments', id] as const,
  analytics:    (days: number) => ['analytics', days] as const,
  transactions: (p?: object) => ['transactions', p] as const,
}

// ── Payments ──────────────────────────────────────────
export function usePayments(params?: { page?: number; size?: number; status?: string }) {
  return useQuery<PageResponse<Payment>>({
    queryKey: queryKeys.payments(params),
    queryFn: () => paymentService.getPayments(params),
  })
}

export function usePayment(id: string) {
  return useQuery<Payment>({
    queryKey: queryKeys.payment(id),
    queryFn:  () => paymentService.getPayment(id),
    enabled:  !!id,
  })
}

export function useCreatePayment() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({
      data,
      idempotencyKey,
    }: {
      data: import('../types').CreatePaymentRequest
      idempotencyKey?: string
    }) => paymentService.createPayment(data, idempotencyKey),
    onSuccess: () => {
      toast.success('Payment initiated successfully!')
      qc.invalidateQueries({ queryKey: ['payments'] })
      qc.invalidateQueries({ queryKey: ['analytics'] })
    },
    onError: (err: any) => {
      const msg = err.response?.data?.message ?? 'Failed to create payment'
      toast.error(msg)
    },
  })
}

export function useRetryPayment() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => paymentService.retryPayment(id),
    onSuccess: () => {
      toast.success('Payment retry initiated!')
      qc.invalidateQueries({ queryKey: ['payments'] })
    },
    onError: (err: any) => {
      toast.error(err.response?.data?.message ?? 'Retry failed')
    },
  })
}

export function useReversePayment() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => paymentService.reversePayment(id),
    onSuccess: () => {
      toast.success('Payment reversed successfully!')
      qc.invalidateQueries({ queryKey: ['payments'] })
    },
    onError: (err: any) => {
      toast.error(err.response?.data?.message ?? 'Reversal failed')
    },
  })
}

// ── Analytics ─────────────────────────────────────────
export function useAnalytics(days = 30) {
  return useQuery<Analytics>({
    queryKey: queryKeys.analytics(days),
    queryFn:  () => paymentService.getAnalytics(days),
    staleTime: 60_000,
  })
}

// ── Transactions ──────────────────────────────────────
export function useTransactions(params?: { page?: number; size?: number }) {
  return useQuery<PageResponse<Transaction>>({
    queryKey: queryKeys.transactions(params),
    queryFn:  () => transactionService.getTransactions(params),
  })
}
