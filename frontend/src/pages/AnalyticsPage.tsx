import { useState } from 'react'
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
  LineChart, Line, Legend, AreaChart, Area,
} from 'recharts'
import { useAnalytics } from '../hooks/usePayments'
import { formatAmount, formatNumber, formatPercent } from '../utils'

const DAYS_OPTIONS = [7, 14, 30, 90]

export default function AnalyticsPage() {
  const [days, setDays] = useState(30)
  const { data, isLoading } = useAnalytics(days)

  const chartData = data?.dailyStats.map(d => ({
    date:    d.date.slice(5),
    success: d.success,
    failed:  d.failed,
    total:   d.total,
    volume:  Number(d.volume),
  })) ?? []

  const methodData = data
    ? Object.entries(data.paymentMethodBreakdown ?? {}).map(([name, value]) => ({ name, value }))
    : []

  const skeletonCard = (
    <div className="card p-6 animate-pulse">
      <div className="h-3 bg-gray-200 rounded w-32 mb-3" />
      <div className="h-6 bg-gray-200 rounded w-24 mb-1" />
      <div className="h-3 bg-gray-100 rounded w-40" />
    </div>
  )

  return (
    <div className="space-y-8 animate-slide-up">
      <div className="page-header">
        <div>
          <h1 className="page-title">Analytics</h1>
          <p className="page-subtitle">Deep dive into your payment performance</p>
        </div>
        <div className="flex gap-2">
          {DAYS_OPTIONS.map(d => (
            <button key={d} onClick={() => setDays(d)}
              className={`text-xs px-3 py-1.5 rounded-lg font-medium transition-colors ${
                days === d
                  ? 'bg-primary-600 text-white'
                  : 'bg-white text-gray-600 border border-gray-200 hover:bg-gray-50'
              }`}>
              {d}d
            </button>
          ))}
        </div>
      </div>

      {/* KPI cards */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        {isLoading ? Array.from({ length: 4 }).map((_, i) => (
          <div key={i}>{skeletonCard}</div>
        )) : (
          <>
            {[
              { label: 'Total Payments', value: formatNumber(data?.totalPayments ?? 0), sub: `Last ${days} days` },
              { label: 'Success Rate',   value: formatPercent(data?.successRate ?? 0),  sub: `${formatNumber(data?.successfulPayments ?? 0)} successful` },
              { label: 'Total Volume',   value: formatAmount(data?.totalVolume ?? 0),   sub: 'All payments' },
              { label: 'Avg. Amount',    value: formatAmount(data?.averagePaymentAmount ?? 0), sub: 'Per payment' },
            ].map(({ label, value, sub }) => (
              <div key={label} className="card p-5">
                <p className="text-xs font-medium text-gray-500">{label}</p>
                <p className="text-xl font-bold text-gray-900 mt-1">{value}</p>
                <p className="text-xs text-gray-400 mt-0.5">{sub}</p>
              </div>
            ))}
          </>
        )}
      </div>

      {/* Volume chart */}
      <div className="card p-6">
        <h2 className="font-semibold text-gray-900 mb-1">Daily Payment Volume</h2>
        <p className="text-xs text-gray-400 mb-6">Total transaction volume per day (USD)</p>
        {isLoading
          ? <div className="h-64 bg-gray-100 rounded-lg animate-pulse" />
          : (
            <ResponsiveContainer width="100%" height={256}>
              <AreaChart data={chartData}>
                <defs>
                  <linearGradient id="volGrad" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%"  stopColor="#3b82f6" stopOpacity={0.2} />
                    <stop offset="95%" stopColor="#3b82f6" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                <XAxis dataKey="date" tick={{ fontSize: 11 }} stroke="#e5e7eb" />
                <YAxis tick={{ fontSize: 11 }} stroke="#e5e7eb"
                  tickFormatter={v => `$${(v/1000).toFixed(0)}k`} />
                <Tooltip
                  contentStyle={{ borderRadius: 8, border: '1px solid #e5e7eb', fontSize: 12 }}
                  formatter={(v: number) => [formatAmount(v), 'Volume']}
                />
                <Area type="monotone" dataKey="volume" name="Volume"
                  stroke="#3b82f6" fill="url(#volGrad)" strokeWidth={2} />
              </AreaChart>
            </ResponsiveContainer>
          )
        }
      </div>

      {/* Success vs Failed + Method breakdown */}
      <div className="grid lg:grid-cols-2 gap-6">
        <div className="card p-6">
          <h2 className="font-semibold text-gray-900 mb-1">Success vs Failed</h2>
          <p className="text-xs text-gray-400 mb-6">Daily comparison</p>
          {isLoading
            ? <div className="h-56 bg-gray-100 rounded-lg animate-pulse" />
            : (
              <ResponsiveContainer width="100%" height={224}>
                <BarChart data={chartData} barGap={2}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                  <XAxis dataKey="date" tick={{ fontSize: 11 }} stroke="#e5e7eb" />
                  <YAxis tick={{ fontSize: 11 }} stroke="#e5e7eb" />
                  <Tooltip
                    contentStyle={{ borderRadius: 8, border: '1px solid #e5e7eb', fontSize: 12 }}
                  />
                  <Legend iconType="circle" iconSize={8}
                    formatter={v => <span className="text-xs text-gray-600">{v}</span>} />
                  <Bar dataKey="success" name="Success" fill="#22c55e" radius={[3,3,0,0]} />
                  <Bar dataKey="failed"  name="Failed"  fill="#ef4444" radius={[3,3,0,0]} />
                </BarChart>
              </ResponsiveContainer>
            )
          }
        </div>

        <div className="card p-6">
          <h2 className="font-semibold text-gray-900 mb-1">Total Payments Trend</h2>
          <p className="text-xs text-gray-400 mb-6">Daily payment count</p>
          {isLoading
            ? <div className="h-56 bg-gray-100 rounded-lg animate-pulse" />
            : (
              <ResponsiveContainer width="100%" height={224}>
                <LineChart data={chartData}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                  <XAxis dataKey="date" tick={{ fontSize: 11 }} stroke="#e5e7eb" />
                  <YAxis tick={{ fontSize: 11 }} stroke="#e5e7eb" />
                  <Tooltip
                    contentStyle={{ borderRadius: 8, border: '1px solid #e5e7eb', fontSize: 12 }}
                  />
                  <Line type="monotone" dataKey="total" name="Total"
                    stroke="#8b5cf6" strokeWidth={2.5} dot={false} />
                </LineChart>
              </ResponsiveContainer>
            )
          }
        </div>
      </div>

      {/* Summary cards */}
      <div className="grid md:grid-cols-3 gap-4">
        <div className="card p-5 flex flex-col gap-3">
          <p className="text-sm font-semibold text-gray-700">Successful Payments</p>
          <p className="text-3xl font-bold text-success-600">
            {formatNumber(data?.successfulPayments ?? 0)}
          </p>
          <p className="text-xs text-gray-400">
            Volume: {formatAmount(data?.successVolume ?? 0)}
          </p>
        </div>
        <div className="card p-5 flex flex-col gap-3">
          <p className="text-sm font-semibold text-gray-700">Failed Payments</p>
          <p className="text-3xl font-bold text-danger-600">
            {formatNumber(data?.failedPayments ?? 0)}
          </p>
          <p className="text-xs text-gray-400">
            Retried: {formatNumber(data?.retriedPayments ?? 0)}
          </p>
        </div>
        <div className="card p-5 flex flex-col gap-3">
          <p className="text-sm font-semibold text-gray-700">Pending / Processing</p>
          <p className="text-3xl font-bold text-primary-600">
            {formatNumber(data?.pendingPayments ?? 0)}
          </p>
          <p className="text-xs text-gray-400">Awaiting settlement</p>
        </div>
      </div>
    </div>
  )
}
