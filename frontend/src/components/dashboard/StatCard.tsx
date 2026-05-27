import clsx from 'clsx'

interface StatCardProps {
  title: string
  value: string | number
  subtitle?: string
  icon: React.ReactNode
  trend?: { value: number; label: string }
  color?: 'blue' | 'green' | 'red' | 'yellow' | 'purple'
  loading?: boolean
}

const colorMap = {
  blue:   { bg: 'bg-primary-50', icon: 'text-primary-600',  ring: 'ring-primary-100' },
  green:  { bg: 'bg-success-50', icon: 'text-success-600',  ring: 'ring-success-100' },
  red:    { bg: 'bg-danger-50',  icon: 'text-danger-600',   ring: 'ring-danger-100'  },
  yellow: { bg: 'bg-warning-50', icon: 'text-warning-600',  ring: 'ring-warning-100' },
  purple: { bg: 'bg-purple-50',  icon: 'text-purple-600',   ring: 'ring-purple-100'  },
}

export function StatCard({
  title, value, subtitle, icon, trend, color = 'blue', loading,
}: StatCardProps) {
  const c = colorMap[color]

  if (loading) {
    return (
      <div className="stat-card animate-pulse">
        <div className="h-3 bg-gray-200 rounded w-24 mb-3" />
        <div className="h-7 bg-gray-200 rounded w-32 mb-2" />
        <div className="h-3 bg-gray-100 rounded w-20" />
      </div>
    )
  }

  return (
    <div className="stat-card hover:shadow-card-hover transition-shadow duration-200">
      <div className="flex items-start justify-between">
        <div>
          <p className="text-sm font-medium text-gray-500">{title}</p>
          <p className="text-2xl font-bold text-gray-900 mt-1">{value}</p>
          {subtitle && (
            <p className="text-xs text-gray-400 mt-0.5">{subtitle}</p>
          )}
        </div>
        <div className={clsx(
          'w-10 h-10 rounded-xl flex items-center justify-center ring-4',
          c.bg, c.ring,
        )}>
          <span className={clsx('w-5 h-5', c.icon)}>{icon}</span>
        </div>
      </div>

      {trend && (
        <div className="flex items-center gap-1 mt-3 pt-3 border-t border-gray-100">
          <span className={clsx(
            'text-xs font-semibold',
            trend.value >= 0 ? 'text-success-600' : 'text-danger-600',
          )}>
            {trend.value >= 0 ? '↑' : '↓'} {Math.abs(trend.value)}%
          </span>
          <span className="text-xs text-gray-400">{trend.label}</span>
        </div>
      )}
    </div>
  )
}
