// ── Auth ──────────────────────────────────────────────
export interface User {
  id: string
  username: string
  email: string
  firstName: string
  lastName: string
  fullName: string
  role: 'USER' | 'ADMIN' | 'MERCHANT'
  enabled: boolean
  createdAt: string
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  user: User
  issuedAt: string
}

export interface LoginRequest {
  usernameOrEmail: string
  password: string
}

export interface RegisterRequest {
  username: string
  email: string
  password: string
  firstName: string
  lastName: string
}

// ── Payments ─────────────────────────────────────────
export type PaymentStatus =
  | 'INITIATED'
  | 'PROCESSING'
  | 'SUCCESS'
  | 'FAILED'
  | 'RETRYING'
  | 'REVERSED'
  | 'TIMEOUT'
  | 'CANCELLED'

export type PaymentMethod =
  | 'CARD'
  | 'BANK_TRANSFER'
  | 'UPI'
  | 'WALLET'
  | 'NET_BANKING'

export type Currency = 'USD' | 'EUR' | 'GBP' | 'INR' | 'JPY'

export interface Payment {
  id: string
  paymentReference: string
  userId: string
  username: string
  amount: number
  currency: string
  status: PaymentStatus
  paymentMethod: PaymentMethod
  description?: string
  metadata?: Record<string, unknown>
  idempotencyKey?: string
  retryCount: number
  maxRetries: number
  failureReason?: string
  gatewayReference?: string
  canRetry: boolean
  canReverse: boolean
  initiatedAt: string
  processedAt?: string
  reversedAt?: string
  createdAt: string
  updatedAt: string
  retryLogs?: RetryLog[]
  transactions?: Transaction[]
}

export interface CreatePaymentRequest {
  amount: number
  currency: string
  paymentMethod: PaymentMethod
  description?: string
  metadata?: Record<string, unknown>
}

// ── Transactions ─────────────────────────────────────
export type TransactionType   = 'DEBIT' | 'CREDIT' | 'REVERSAL' | 'FEE' | 'REFUND'
export type TransactionStatus = 'PENDING' | 'COMPLETED' | 'FAILED' | 'REVERSED'

export interface Transaction {
  id: string
  transactionReference: string
  paymentId: string
  paymentReference: string
  userId: string
  type: TransactionType
  amount: number
  currency: string
  status: TransactionStatus
  balanceBefore?: number
  balanceAfter?: number
  description?: string
  createdAt: string
}

// ── Retry Logs ────────────────────────────────────────
export interface RetryLog {
  id: string
  paymentId: string
  attemptNumber: number
  status: string
  errorMessage?: string
  errorCode?: string
  retryAt: string
  nextRetryAt?: string
  createdAt: string
}

// ── Analytics ─────────────────────────────────────────
export interface DailyStats {
  date: string
  total: number
  success: number
  failed: number
  volume: number
}

export interface RecentActivity {
  paymentReference: string
  amount: number
  currency: string
  status: PaymentStatus
  paymentMethod: PaymentMethod
  createdAt: string
}

export interface Analytics {
  totalPayments: number
  successfulPayments: number
  failedPayments: number
  pendingPayments: number
  retriedPayments: number
  totalVolume: number
  successVolume: number
  successRate: number
  averagePaymentAmount: number
  dailyStats: DailyStats[]
  paymentMethodBreakdown: Record<string, number>
  statusBreakdown: Record<string, number>
  recentActivity: RecentActivity[]
  generatedAt: string
}

// ── API ───────────────────────────────────────────────
export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
  correlationId?: string
  timestamp: string
  error?: {
    code: string
    detail?: string
    validationErrors?: Record<string, string>
  }
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  first: boolean
  last: boolean
  empty: boolean
}
