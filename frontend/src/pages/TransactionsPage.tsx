import { useState } from 'react'
import { List, ChevronLeft, ChevronRight, RefreshCw } from 'lucide-react'
import { useTransactions } from '../hooks/usePayments'
import { EmptyState } from '../components/common/EmptyState'
import { formatAmount, formatDate, getTxnTypeClass, getTxnStatusClass } from '../utils'
import clsx from 'clsx'
import type { TransactionType, TransactionStatus } from '../types'

export default function TransactionsPage() {
  const [page, setPage] = useState(0)
  const size = 20

  const { data, isLoading, refetch, isFetching } = useTransactions({ page, size })

  return (
    <div className="space-y-6 animate-slide-up">
      <div className="page-header">
        <div>
          <h1 className="page-title">Transactions</h1>
          <p className="page-subtitle">Complete ledger of all transaction records</p>
        </div>
        <button onClick={() => refetch()} disabled={isFetching} className="btn-secondary">
          <RefreshCw className={clsx('w-4 h-4', isFetching && 'animate-spin')} />
          Refresh
        </button>
      </div>

      <div className="table-container">
        <table className="table">
          <thead>
            <tr>
              <th>Reference</th>
              <th>Payment Ref</th>
              <th>Type</th>
              <th>Amount</th>
              <th>Status</th>
              <th>Description</th>
              <th>Date</th>
            </tr>
          </thead>
          <tbody>
            {isLoading
              ? Array.from({ length: 10 }).map((_, i) => (
                  <tr key={i}>
                    {Array.from({ length: 7 }).map((_, j) => (
                      <td key={j}><div className="h-3 bg-gray-100 rounded animate-pulse" /></td>
                    ))}
                  </tr>
                ))
              : data?.content.map(txn => (
                  <tr key={txn.id}>
                    <td>
                      <span className="font-mono text-xs text-gray-500">
                        {txn.transactionReference}
                      </span>
                    </td>
                    <td>
                      <span className="font-mono text-xs text-gray-400">
                        {txn.paymentReference}
                      </span>
                    </td>
                    <td>
                      <span className={clsx(
                        'text-xs font-semibold',
                        getTxnTypeClass(txn.type as TransactionType),
                      )}>
                        {txn.type === 'DEBIT'    && '↓ '}
                        {txn.type === 'CREDIT'   && '↑ '}
                        {txn.type === 'REVERSAL' && '↺ '}
                        {txn.type}
                      </span>
                    </td>
                    <td>
                      <span className={clsx(
                        'font-semibold text-sm',
                        txn.type === 'DEBIT' ? 'text-danger-600' : 'text-success-600',
                      )}>
                        {txn.type === 'DEBIT' ? '−' : '+'}
                        {formatAmount(txn.amount, txn.currency)}
                      </span>
                    </td>
                    <td>
                      <span className={getTxnStatusClass(txn.status as TransactionStatus)}>
                        {txn.status}
                      </span>
                    </td>
                    <td className="text-gray-500 text-xs max-w-xs truncate">
                      {txn.description ?? '—'}
                    </td>
                    <td className="text-xs text-gray-400">{formatDate(txn.createdAt)}</td>
                  </tr>
                ))
            }
          </tbody>
        </table>

        {!isLoading && !data?.content.length && (
          <EmptyState
            icon={<List className="w-6 h-6" />}
            title="No transactions yet"
            description="Transactions appear here once payments are processed"
          />
        )}
      </div>

      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-between text-sm text-gray-500">
          <span>
            {page * size + 1}–{Math.min((page + 1) * size, data.totalElements)} of{' '}
            {data.totalElements}
          </span>
          <div className="flex gap-2">
            <button disabled={data.first} onClick={() => setPage(p => p - 1)}
              className="btn-secondary py-1.5 px-3 disabled:opacity-40">
              <ChevronLeft className="w-4 h-4" />
            </button>
            <span className="px-3 py-1.5 bg-white border border-gray-200 rounded-lg font-medium">
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
