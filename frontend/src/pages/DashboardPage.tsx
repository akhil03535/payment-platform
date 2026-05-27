import { useState } from 'react'
import {
  CreditCard, TrendingUp, AlertCircle, RefreshCw,
  DollarSign, Activity, ArrowUpRight,
} from 'lucide-react'
import {
  AreaChart, Area, XAxis, YAxis, CartesianGrid,
  Tooltip, ResponsiveContainer, PieChart, Pie, Cell, Legend,
} from 'recharts'
import { useAnalytics } from '../hooks/usePayments'
import { useAuth } from '../context/AuthContext'
import { StatCard } from '../components/dashboard/StatCard'
import { StatusBadge } from '../components/common/StatusBadge'
import { formatAmount, formatDate, formatPercent, formatNumber } from '../utils'
import type { PaymentStatus } from '../types'
import { Link } from 'react-router-dom'

const DAYS_OPTIONS = [7, 30, 90]

const PIE_COLORS: Record<string, string> = {
  SUCCESS:    '#22c55e',
  FAILED:     '#ef4444',
  PROCESSING: '#3b82f6',
  INITIATED:  '#94a3b8',
  RETRYING:   '#f59e0b',
  REVERSED:   '#6b7280',
  TIMEOUT:    '#d97706',
  CANCELLED:  '#9ca3af',
}

export default function DashboardPage() {
  const { user } = useAuth()
  const [days, setDays] = useState(30)
  const { data: analytics, isLoading } = useAnalytics(days)

  const pieData = analytics
    ? Object.entries(analytics.statusBreakdown).map(([name, value]) => ({ name, value }))
    : []

  const chartData = analytics?.dailyStats.map(d => ({
    date:    d.date.slice(5),   // MM-DD
    total:   d.total,
    success: d.success,
    failed:  d.failed,
    volume:  Number(d.volume),
  })) ?? []

  return (
    <div className="space-y-8 animate-slide-up">
      {/* Header */}
      <div className="page-header">
        <div>
          <h1 className="page-title">Welcome back, {user?.firstName} 👋</h1>
          <p className="page-subtitle">Here's what's happening with your payments</p>
        </div>
        <div className="flex items-center gap-2">
          {DAYS_OPTIONS.map(d => (
            <button
              key={d}
              onClick={() => setDays(d)}
              className={`text-xs px-3 py-1.5 rounded-lg font-medium transition-colors ${
                days === d
                  ? 'bg-primary-600 text-white'
                  : 'bg-white text-gray-600 border border-gray-200 hover:bg-gray-50'
              }`}
            >
              {d}d
            </button>
          ))}
        </div>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          title="Total Payments"
          value={formatNumber(analytics?.totalPayments ?? 0)}
          subtitle={`Last ${days} days`}
          icon={<CreditCard className="w-full h-full" />}
          color="blue"
          loading={isLoading}
        />
        <StatCard
          title="Total Volume"
          value={formatAmount(analytics?.totalVolume ?? 0)}
          subtitle="Successful payments"
          icon={<DollarSign className="w-full h-full" />}
          color="green"
          loading={isLoading}
        />
        <StatCard
          title="Success Rate"
          value={formatPercent(analytics?.successRate ?? 0)}
          subtitle={`${formatNumber(analytics?.successfulPayments ?? 0)} successful`}
          icon={<TrendingUp className="w-full h-full" />}
          color={
            (analytics?.successRate ?? 0) >= 90 ? 'green' :
            (analytics?.successRate ?? 0) >= 70 ? 'yellow' : 'red'
          }
          loading={isLoading}
        />
        <StatCard
          title="Failed Payments"
          value={formatNumber(analytics?.failedPayments ?? 0)}
          subtitle={`${formatNumber(analytics?.retriedPayments ?? 0)} retried`}
          icon={<AlertCircle className="w-full h-full" />}
          color="red"
          loading={isLoading}
        />
      </div>

      {/* Charts row */}
      <div className="grid lg:grid-cols-3 gap-6">
        {/* Area chart */}
        <div className="card p-6 lg:col-span-2">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h2 className="font-semibold text-gray-900">Payment Trends</h2>
              <p className="text-xs text-gray-400 mt-0.5">Daily breakdown</p>
            </div>
            <Activity className="w-4 h-4 text-gray-400" />
          </div>
          {isLoading ? (
            <div className="h-56 bg-gray-100 rounded-lg animate-pulse" />
          ) : (
            <ResponsiveContainer width="100%" height={224}>
              <AreaChart data={chartData}>
                <defs>
                  <linearGradient id="gradSuccess" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%"  stopColor="#22c55e" stopOpacity={0.15} />
                    <stop offset="95%" stopColor="#22c55e" stopOpacity={0} />
                  </linearGradient>
                  <linearGradient id="gradFailed" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%"  stopColor="#ef4444" stopOpacity={0.15} />
                    <stop offset="95%" stopColor="#ef4444" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                <XAxis dataKey="date" tick={{ fontSize: 11 }} stroke="#e5e7eb" />
                <YAxis tick={{ fontSize: 11 }} stroke="#e5e7eb" />
                <Tooltip
                  contentStyle={{ borderRadius: 8, border: '1px solid #e5e7eb', fontSize: 12 }}
                />
                <Area type="monotone" dataKey="success" name="Success"
                  stroke="#22c55e" fill="url(#gradSuccess)" strokeWidth={2} />
                <Area type="monotone" dataKey="failed" name="Failed"
                  stroke="#ef4444" fill="url(#gradFailed)" strokeWidth={2} />
              </AreaChart>
            </ResponsiveContainer>
          )}
        </div>

        {/* Pie chart */}
        <div className="card p-6">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h2 className="font-semibold text-gray-900">Status Breakdown</h2>
              <p className="text-xs text-gray-400 mt-0.5">By payment status</p>
            </div>
          </div>
          {isLoading ? (
            <div className="h-56 bg-gray-100 rounded-lg animate-pulse" />
          ) : pieData.length > 0 ? (
            <ResponsiveContainer width="100%" height={224}>
              <PieChart>
                <Pie
                  data={pieData}
                  cx="50%" cy="50%"
                  innerRadius={55} outerRadius={85}
                  paddingAngle={3}
                  dataKey="value"
                >
                  {pieData.map((entry) => (
                    <Cell key={entry.name}
                      fill={PIE_COLORS[entry.name] ?? '#94a3b8'} />
                  ))}
                </Pie>
                <Tooltip
                  contentStyle={{ borderRadius: 8, border: '1px solid #e5e7eb', fontSize: 12 }}
                />
                <Legend iconType="circle" iconSize={8}
                  formatter={(v) => <span className="text-xs text-gray-600">{v}</span>} />
              </PieChart>
            </ResponsiveContainer>
          ) : (
            <div className="h-56 flex items-center justify-center text-gray-400 text-sm">
              No data yet
            </div>
          )}
        </div>
      </div>

      {/* Recent activity */}
      <div className="card">
        <div className="flex items-center justify-between p-5 border-b border-gray-100">
          <div>
            <h2 className="font-semibold text-gray-900">Recent Payments</h2>
            <p className="text-xs text-gray-400 mt-0.5">Latest transactions</p>
          </div>
          <Link to="/payments" className="text-xs font-semibold text-primary-600
                                          hover:text-primary-700 flex items-center gap-1">
            View all <ArrowUpRight className="w-3 h-3" />
          </Link>
        </div>

        <div className="divide-y divide-gray-50">
          {isLoading
            ? Array.from({ length: 5 }).map((_, i) => (
                <div key={i} className="flex items-center gap-4 px-5 py-3.5 animate-pulse">
                  <div className="w-24 h-3 bg-gray-200 rounded" />
                  <div className="flex-1 h-3 bg-gray-100 rounded" />
                  <div className="w-16 h-3 bg-gray-200 rounded" />
                </div>
              ))
            : analytics?.recentActivity.map((act) => (
                <div key={act.paymentReference}
                  className="flex items-center gap-4 px-5 py-3.5 hover:bg-gray-50 transition-colors">
                  <span className="font-mono text-xs text-gray-500 w-36 shrink-0">
                    {act.paymentReference}
                  </span>
                  <div className="flex-1 min-w-0">
                    <StatusBadge status={act.status as PaymentStatus} />
                  </div>
                  <span className="text-sm font-semibold text-gray-900">
                    {formatAmount(act.amount, act.currency)}
                  </span>
                  <span className="text-xs text-gray-400 w-32 text-right shrink-0 hidden md:block">
                    {formatDate(act.createdAt)}
                  </span>
                </div>
              ))
          }
          {!isLoading && !analytics?.recentActivity.length && (
            <div className="px-5 py-8 text-center text-sm text-gray-400">
              No payments yet. <Link to="/payments" className="text-primary-600 font-medium">
                Create your first one →
              </Link>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
