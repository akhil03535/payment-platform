import { useState } from 'react'
import { Link, useNavigate, useLocation } from 'react-router-dom'
import { Zap, Eye, EyeOff, Loader2 } from 'lucide-react'
import { useAuth } from '../context/AuthContext'
import toast from 'react-hot-toast'

export default function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const from = (location.state as any)?.from?.pathname ?? '/dashboard'

  const [form, setForm] = useState({ usernameOrEmail: '', password: '' })
  const [showPw, setShowPw] = useState(false)
  const [loading, setLoading] = useState(false)
  const [errors, setErrors] = useState<Record<string, string>>({})

  const validate = () => {
    const e: Record<string, string> = {}
    if (!form.usernameOrEmail) e.usernameOrEmail = 'Username or email is required'
    if (!form.password) e.password = 'Password is required'
    setErrors(e)
    return Object.keys(e).length === 0
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!validate()) return
    setLoading(true)
    try {
      await login(form)
      toast.success('Welcome back!')
      navigate(from, { replace: true })
    } catch (err: any) {
      const msg = err.response?.data?.message ?? 'Invalid credentials'
      toast.error(msg)
    } finally {
      setLoading(false)
    }
  }

  const fillDemo = () =>
    setForm({ usernameOrEmail: 'demo', password: 'Demo@123456' })

  return (
    <div className="min-h-screen bg-gradient-to-br from-primary-50 via-white to-blue-50
                    flex items-center justify-center p-4">
      <div className="w-full max-w-md animate-slide-up">
        {/* Header */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-12 h-12
                          bg-primary-600 rounded-2xl shadow-lg mb-4">
            <Zap className="w-6 h-6 text-white" />
          </div>
          <h1 className="text-2xl font-bold text-gray-900">Sign in to PayFlow</h1>
          <p className="text-sm text-gray-500 mt-1">
            Enterprise payment processing platform
          </p>
        </div>

        {/* Demo hint */}
        <div className="mb-6 p-3 bg-primary-50 border border-primary-100 rounded-xl
                        flex items-center justify-between gap-3">
          <div>
            <p className="text-xs font-semibold text-primary-700">Demo credentials</p>
            <p className="text-xs text-primary-600">demo / Demo@123456</p>
          </div>
          <button
            type="button"
            onClick={fillDemo}
            className="text-xs font-semibold text-primary-600 hover:text-primary-800
                       underline underline-offset-2 shrink-0"
          >
            Fill in
          </button>
        </div>

        {/* Card */}
        <div className="card p-8">
          <form onSubmit={handleSubmit} className="space-y-5" noValidate>
            <div>
              <label className="label">Username or Email</label>
              <input
                type="text"
                autoComplete="username"
                value={form.usernameOrEmail}
                onChange={e => setForm(f => ({ ...f, usernameOrEmail: e.target.value }))}
                className={errors.usernameOrEmail ? 'input-error' : 'input'}
                placeholder="john_doe or john@example.com"
              />
              {errors.usernameOrEmail && (
                <p className="mt-1 text-xs text-danger-600">{errors.usernameOrEmail}</p>
              )}
            </div>

            <div>
              <label className="label">Password</label>
              <div className="relative">
                <input
                  type={showPw ? 'text' : 'password'}
                  autoComplete="current-password"
                  value={form.password}
                  onChange={e => setForm(f => ({ ...f, password: e.target.value }))}
                  className={`${errors.password ? 'input-error' : 'input'} pr-10`}
                  placeholder="••••••••"
                />
                <button
                  type="button"
                  onClick={() => setShowPw(v => !v)}
                  className="absolute inset-y-0 right-0 flex items-center pr-3
                             text-gray-400 hover:text-gray-600"
                >
                  {showPw ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
              {errors.password && (
                <p className="mt-1 text-xs text-danger-600">{errors.password}</p>
              )}
            </div>

            <button
              type="submit"
              disabled={loading}
              className="btn-primary w-full justify-center py-2.5 text-base"
            >
              {loading
                ? <><Loader2 className="w-4 h-4 animate-spin" /> Signing in…</>
                : 'Sign in'
              }
            </button>
          </form>

          <p className="mt-6 text-center text-sm text-gray-500">
            No account?{' '}
            <Link to="/register" className="font-semibold text-primary-600 hover:text-primary-700">
              Create one free
            </Link>
          </p>
        </div>
      </div>
    </div>
  )
}
