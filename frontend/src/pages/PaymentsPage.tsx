import { useState } from 'react'
import { Plus, Search, Filter, RefreshCw, RotateCcw,
         CreditCard, Loader2, X, ChevronLeft, ChevronRight } from 'lucide-react'
import { usePayments, useCreatePayment, useRetryPayment, useReversePayment } from '../hooks/usePayments'
import { StatusBadge } from '../components/common/StatusBadge'
import { EmptyState } from '../components/common/EmptyState'
import { formatAmount, formatDate, generateIdempotencyKey,
         PAYMENT_METHOD_LABELS } from '../utils'
import type { Payment, CreatePaymentRequest, PaymentMethod } from '../types'
import toast from 'react-hot-toast'
import clsx from 'clsx'

const STATUSES = ['', 'INITIATED','PROCESSING','SUCCESS','FAILED','RETRYING','REVERSED','TIMEOUT']
const CURRENCIES = ['USD','EUR','GBP','INR']
const METHODS: PaymentMethod[] = ['CARD','BANK_TRANSFER','UPI','WALLET','NET_BANKING']

function CreatePaymentModal({ onClose }: { onClose: () => void }) {
  const createPayment = useCreatePayment()
  const [form, setForm] = useState<CreatePaymentRequest & { idempotent: boolean }>({
    amount: 0, currency: 'USD', paymentMethod: 'CARD', description: '', idempotent: true,
  })
  const [errors, setErrors] = useState<Record<string, string>>({})

  const validate = () => {
    const e: Record<string, string> = {}
    if (!form.amount || form.amount <= 0) e.amount = 'Amount must be > 0'
    if (form.amount > 999999) e.amount = 'Max amount is 999,999'
    if (!form.paymentMethod) e.paymentMethod = 'Required'
    setErrors(e)
    return Object.keys(e).length === 0
  }

  const handleSubmit = async (ev: React.FormEvent) => {
    ev.preventDefault()
    if (!validate()) return
    const key = form.idempotent ? generateIdempotencyKey() : undefined
    await createPayment.mutateAsync({
      data: { amount: form.amount, currency: form.currency,
              paymentMethod: form.paymentMethod, description: form.description },
      idempotencyKey: key,
    })
    onClose()
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/50 backdrop-blur-sm" onClick={onClose} />
      <div className="relative w-full max-w-md card p-6 animate-slide-up">
        <div className="flex items-center justify-between mb-5">
          <h2 className="text-lg font-semibold text-gray-900">New Payment</h2>
          <button onClick={onClose} className="p-1 rounded-lg hover:bg-gray-100 text-gray-400">
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Amount + Currency */}
          <div className="flex gap-3">
            <div className="flex-1">
              <label className="label">Amount</label>
              <input type="number" step="0.01" min="0.01"
                className={errors.amount ? 'input-error' : 'input'}
                value={form.amount || ''}
                onChange={e => setForm(f => ({ ...f, amount: parseFloat(e.target.value) || 0 }))}
                placeholder="100.00"
              />
              {errors.amount && <p className="mt-1 text-xs text-danger-600">{errors.amount}</p>}
            </div>
            <div className="w-28">
              <label className="label">Currency</label>
              <select className="input" value={form.currency}
                onChange={e => setForm(f => ({ ...f, currency: e.target.value }))}>
                {CURRENCIES.map(c => <option key={c}>{c}</option>)}
              </select>
            </div>
          </div>

          <div>
            <label className="label">Payment Method</label>
            <div className="grid grid-cols-2 gap-2">
              {METHODS.map(m => (
                <button key={m} type="button"
                  onClick={() => setForm(f => ({ ...f, paymentMethod: m }))}
                  className={clsx(
                    'px-3 py-2 rounded-lg border text-xs font-medium text-left transition-colors',
                    form.paymentMethod === m
                      ? 'border-primary-500 bg-primary-50 text-primary-700'
                      : 'border-gray-200 hover:border-gray-300 text-gray-600',
                  )}>
                  {PAYMENT_METHOD_LABELS[m]}
                </button>
              ))}
            </div>
            {errors.paymentMethod && (
              <p className="mt-1 text-xs text-danger-600">{errors.paymentMethod}</p>
            )}
          </div>

          <div>
            <label className="label">Description <span className="text-gray-400">(optional)</span></label>
            <input type="text" className="input"
              value={form.description}
              onChange={e => setForm(f => ({ ...f, description: e.target.value }))}
              placeholder="e.g. Invoice #1234"
            />
          </div>

          <label className="flex items-center gap-2 cursor-pointer">
            <input type="checkbox" className="rounded"
              checked={form.idempotent}
              onChange={e => setForm(f => ({ ...f, idempotent: e.target.checked }))}
            />
            <span className="text-sm text-gray-600">Enable idempotency (prevent duplicates)</span>
          </label>

          <div className="flex gap-3 pt-2">
            <button type="button" onClick={onClose} className="btn-secondary flex-1 justify-center">
              Cancel
            </button>
            <button type="submit" disabled={createPayment.isPending}
              className="btn-primary flex-1 justify-center">
              {createPayment.isPending
                ? <><Loader2 className="w-4 h-4 animate-spin" /> Processing…</>
                : 'Create Payment'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

function PaymentRow({ payment }: { payment: Payment }) {
  const retry   = useRetryPayment()
  const reverse = useReversePayment()

  return (
    <tr>
      <td>
        <span className="font-mono text-xs text-gray-500">{payment.paymentReference}</span>
      </td>
      <td>
        <StatusBadge status={payment.status} pulse />
      </td>
      <td className="font-semibold text-gray-900">
        {formatAmount(payment.amount, payment.currency)}
      </td>
      <td className="text-gray-500 text-xs">
        {PAYMENT_METHOD_LABELS[payment.paymentMethod] ?? payment.paymentMethod}
      </td>
      <td className="text-xs text-gray-400">{formatDate(payment.createdAt)}</td>
      <td>
        <span className={clsx(
          'text-xs font-medium',
          payment.retryCount > 0 ? 'text-warning-600' : 'text-gray-400',
        )}>
          {payment.retryCount}/{payment.maxRetries}
        </span>
      </td>
      <td>
        <div className="flex items-center gap-1.5">
          {payment.canRetry && (
            <button
              onClick={() => retry.mutate(payment.id)}
              disabled={retry.isPending}
              className="btn-sm bg-warning-50 text-warning-700 border-0 hover:bg-warning-100"
              title="Retry payment"
            >
              <RefreshCw className="w-3 h-3" />
              Retry
            </button>
          )}
          {payment.canReverse && (
            <button
              onClick={() => {
                if (confirm('Reverse this payment?')) reverse.mutate(payment.id)
              }}
              disabled={reverse.isPending}
              className="btn-sm bg-gray-50 text-gray-600 border-0 hover:bg-gray-100"
              title="Reverse payment"
            >
              <RotateCcw className="w-3 h-3" />
              Reverse
            </button>
          )}
        </div>
      </td>
    </tr>
  )
}

export default function PaymentsPage() {
  const [showCreate, setShowCreate] = useState(false)
  const [page, setPage] = useState(0)
  const [status, setStatus] = useState('')
  const [search, setSearch] = useState('')
  const size = 15

  const { data, isLoading, refetch, isFetching } = usePayments({ page, size, status: status || undefined })

  const filtered = data?.content.filter(p =>
    !search ||
    p.paymentReference.toLowerCase().includes(search.toLowerCase()) ||
    p.username.toLowerCase().includes(search.toLowerCase())
  ) ?? []

  return (
    <div className="space-y-6 animate-slide-up">
      {showCreate && <CreatePaymentModal onClose={() => setShowCreate(false)} />}

      <div className="page-header">
        <div>
          <h1 className="page-title">Payments</h1>
          <p className="page-subtitle">Manage and monitor all payment transactions</p>
        </div>
        <button onClick={() => setShowCreate(true)} className="btn-primary">
          <Plus className="w-4 h-4" /> New Payment
        </button>
      </div>

      {/* Filters */}
      <div className="card p-4 flex flex-wrap gap-3">
        <div className="relative flex-1 min-w-48">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
          <input className="input pl-9" placeholder="Search by reference or user…"
            value={search} onChange={e => setSearch(e.target.value)} />
        </div>
        <select className="input w-44" value={status}
          onChange={e => { setStatus(e.target.value); setPage(0) }}>
          {STATUSES.map(s => <option key={s} value={s}>{s || 'All statuses'}</option>)}
        </select>
        <button onClick={() => refetch()} disabled={isFetching}
          className="btn-secondary gap-2">
          <RefreshCw className={clsx('w-4 h-4', isFetching && 'animate-spin')} />
          Refresh
        </button>
      </div>

      {/* Table */}
      <div className="table-container">
        <table className="table">
          <thead>
            <tr>
              <th>Reference</th>
              <th>Status</th>
              <th>Amount</th>
              <th>Method</th>
              <th>Created</th>
              <th>Retries</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {isLoading
              ? Array.from({ length: 8 }).map((_, i) => (
                  <tr key={i}>
                    {Array.from({ length: 7 }).map((_, j) => (
                      <td key={j}><div className="h-3 bg-gray-100 rounded animate-pulse" /></td>
                    ))}
                  </tr>
                ))
              : filtered.map(p => <PaymentRow key={p.id} payment={p} />)
            }
          </tbody>
        </table>

        {!isLoading && filtered.length === 0 && (
          <EmptyState
            icon={<CreditCard className="w-6 h-6" />}
            title="No payments found"
            description={search || status ? 'Try adjusting your filters' : 'Create your first payment to get started'}
            action={!search && !status
              ? <button onClick={() => setShowCreate(true)} className="btn-primary">
                  <Plus className="w-4 h-4" /> New Payment
                </button>
              : undefined}
          />
        )}
      </div>

      {/* Pagination */}
      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-between text-sm text-gray-500">
          <span>
            Showing {page * size + 1}–{Math.min((page + 1) * size, data.totalElements)} of{' '}
            {data.totalElements} payments
          </span>
          <div className="flex gap-2">
            <button disabled={data.first} onClick={() => setPage(p => p - 1)}
              className="btn-secondary py-1.5 px-3 disabled:opacity-40">
              <ChevronLeft className="w-4 h-4" />
            </button>
            <span className="px-3 py-1.5 rounded-lg bg-white border border-gray-200 font-medium">
              {page + 1} / {data.totalPages}
            </span>
            <button disabled={data.last} onClick={() => setPage(p => p + 1)}
              className="btn-secondary py-1.5 px-3 disabled:opacity-40">
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
