import { useAuth } from '../context/AuthContext'
import { formatDate } from '../utils'
import { User, Mail, Shield, Calendar, LogOut } from 'lucide-react'
import { useNavigate } from 'react-router-dom'

export default function ProfilePage() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = async () => {
    await logout()
    navigate('/login')
  }

  if (!user) return null

  const infoRow = (icon: React.ReactNode, label: string, value: string) => (
    <div className="flex items-start gap-4 py-4 border-b border-gray-100 last:border-0">
      <div className="w-8 h-8 rounded-lg bg-gray-100 flex items-center justify-center
                      text-gray-500 shrink-0 mt-0.5">
        {icon}
      </div>
      <div>
        <p className="text-xs font-medium text-gray-400 mb-0.5">{label}</p>
        <p className="text-sm font-medium text-gray-900">{value}</p>
      </div>
    </div>
  )

  return (
    <div className="space-y-6 animate-slide-up max-w-lg">
      <div className="page-header">
        <div>
          <h1 className="page-title">Profile</h1>
          <p className="page-subtitle">Your account information</p>
        </div>
      </div>

      {/* Avatar card */}
      <div className="card p-6 flex items-center gap-5">
        <div className="w-16 h-16 rounded-2xl bg-primary-100 flex items-center justify-center
                        text-primary-700 font-bold text-xl">
          {user.firstName[0]}{user.lastName[0]}
        </div>
        <div>
          <h2 className="text-lg font-bold text-gray-900">{user.fullName}</h2>
          <p className="text-sm text-gray-400">@{user.username}</p>
          <span className="mt-1 badge badge-primary">{user.role}</span>
        </div>
      </div>

      {/* Info */}
      <div className="card p-6">
        <h3 className="text-sm font-semibold text-gray-700 mb-2">Account Details</h3>
        {infoRow(<User className="w-4 h-4" />, 'Full Name', user.fullName)}
        {infoRow(<Mail className="w-4 h-4" />, 'Email', user.email)}
        {infoRow(<Shield className="w-4 h-4" />, 'Role', user.role)}
        {infoRow(<Calendar className="w-4 h-4" />, 'Member Since', formatDate(user.createdAt))}
        {infoRow(
          <span className={`w-2 h-2 rounded-full mt-1 ${user.enabled ? 'bg-success-500' : 'bg-gray-400'}`} />,
          'Account Status',
          user.enabled ? 'Active' : 'Inactive',
        )}
      </div>

      {/* Actions */}
      <button onClick={handleLogout}
        className="btn-danger w-full justify-center py-2.5">
        <LogOut className="w-4 h-4" />
        Sign out
      </button>
    </div>
  )
}
