import { format, formatDistanceToNow } from 'date-fns'
import type { PaymentStatus, TransactionType, TransactionStatus } from '../types'

// ── Currency ──────────────────────────────────────────
export function formatAmount(amount: number, currency = 'USD'): string {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency,
    minimumFractionDigits: 2,
  }).format(amount)
}

// ── Dates ─────────────────────────────────────────────
export function formatDate(iso: string | undefined | null): string {
  if (!iso) return '—'
  try { return format(new Date(iso), 'MMM dd, yyyy HH:mm') } catch { return '—' }
}

export function formatDateShort(iso: string | undefined | null): string {
  if (!iso) return '—'
  try { return format(new Date(iso), 'MMM dd, yyyy') } catch { return '—' }
}

export function timeAgo(iso: string | undefined | null): string {
  if (!iso) return '—'
  try { return formatDistanceToNow(new Date(iso), { addSuffix: true }) } catch { return '—' }
}

// ── Status colours ────────────────────────────────────
export function getPaymentStatusClass(status: PaymentStatus): string {
  const map: Record<PaymentStatus, string> = {
    SUCCESS:    'badge-success',
    FAILED:     'badge-danger',
    PROCESSING: 'badge-primary',
    INITIATED:  'badge-gray',
    RETRYING:   'badge-warning',
    REVERSED:   'badge-gray',
    TIMEOUT:    'badge-warning',
    CANCELLED:  'badge-gray',
  }
  return map[status] ?? 'badge-gray'
}

export function getPaymentStatusDot(status: PaymentStatus): string {
  const map: Record<PaymentStatus, string> = {
    SUCCESS:    'bg-success-500',
    FAILED:     'bg-danger-500',
    PROCESSING: 'bg-primary-500',
    INITIATED:  'bg-gray-400',
    RETRYING:   'bg-warning-500',
    REVERSED:   'bg-gray-500',
    TIMEOUT:    'bg-warning-600',
    CANCELLED:  'bg-gray-400',
  }
  return map[status] ?? 'bg-gray-400'
}

export function getTxnTypeClass(type: TransactionType): string {
  const map: Record<TransactionType, string> = {
    DEBIT:    'text-danger-600',
    CREDIT:   'text-success-600',
    REVERSAL: 'text-warning-600',
    FEE:      'text-gray-600',
    REFUND:   'text-primary-600',
  }
  return map[type] ?? 'text-gray-600'
}

export function getTxnStatusClass(status: TransactionStatus): string {
  const map: Record<TransactionStatus, string> = {
    COMPLETED: 'badge-success',
    PENDING:   'badge-warning',
    FAILED:    'badge-danger',
    REVERSED:  'badge-gray',
  }
  return map[status] ?? 'badge-gray'
}

// ── Payment method labels ─────────────────────────────
export const PAYMENT_METHOD_LABELS: Record<string, string> = {
  CARD:         '💳 Card',
  BANK_TRANSFER:'🏦 Bank Transfer',
  UPI:          '📱 UPI',
  WALLET:       '👛 Wallet',
  NET_BANKING:  '🌐 Net Banking',
}

// ── Idempotency key generator ─────────────────────────
export function generateIdempotencyKey(): string {
  return `${Date.now()}-${Math.random().toString(36).substring(2, 9)}`
}

// ── Truncate ──────────────────────────────────────────
export function truncate(str: string, n = 20): string {
  return str.length > n ? str.slice(0, n) + '…' : str
}

// ── Number ────────────────────────────────────────────
export function formatNumber(n: number): string {
  return new Intl.NumberFormat('en-US').format(n)
}

export function formatPercent(n: number): string {
  return `${n.toFixed(1)}%`
}
