import clsx from 'clsx'
import type { PaymentStatus } from '../../types'
import { getPaymentStatusClass } from '../../utils'

interface Props {
  status: PaymentStatus
  pulse?: boolean
}

export function StatusBadge({ status, pulse }: Props) {
  const isActive = status === 'PROCESSING' || status === 'RETRYING'

  return (
    <span className={clsx(getPaymentStatusClass(status), 'inline-flex items-center gap-1.5')}>
      <span className={clsx(
        'w-1.5 h-1.5 rounded-full',
        status === 'SUCCESS'    && 'bg-success-500',
        status === 'FAILED'     && 'bg-danger-500',
        status === 'PROCESSING' && 'bg-primary-500',
        status === 'RETRYING'   && 'bg-warning-500',
        status === 'REVERSED'   && 'bg-gray-500',
        status === 'TIMEOUT'    && 'bg-warning-600',
        status === 'INITIATED'  && 'bg-gray-400',
        status === 'CANCELLED'  && 'bg-gray-400',
        (pulse && isActive) && 'animate-pulse',
      )} />
      {status}
    </span>
  )
}
