import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Zap, Eye, EyeOff, Loader2 } from 'lucide-react'
import { useAuth } from '../context/AuthContext'
import toast from 'react-hot-toast'

interface FormState {
  username: string; email: string; password: string
  firstName: string; lastName: string
}

export default function RegisterPage() {
  const { register } = useAuth()
  const navigate = useNavigate()

  const [form, setForm] = useState<FormState>({
    username: '', email: '', password: '', firstName: '', lastName: '',
  })
  const [showPw, setShowPw] = useState(false)
  const [loading, setLoading] = useState(false)
  const [errors, setErrors] = useState<Partial<FormState>>({})

  const set = (k: keyof FormState) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setForm(f => ({ ...f, [k]: e.target.value }))

  const validate = () => {
    const e: Partial<FormState> = {}
    if (!form.firstName) e.firstName = 'Required'
    if (!form.lastName)  e.lastName  = 'Required'
    if (!form.username || form.username.length < 3) e.username = 'Min 3 characters'
    if (!/^[a-zA-Z0-9_]+$/.test(form.username)) e.username = 'Letters, numbers, underscores only'
    if (!form.email || !/\S+@\S+\.\S+/.test(form.email)) e.email = 'Valid email required'
    const pwOk = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&]).{8,}$/.test(form.password)
    if (!pwOk) e.password = 'Min 8 chars, uppercase, number & special char'
    setErrors(e)
    return Object.keys(e).length === 0
  }

  const handleSubmit = async (ev: React.FormEvent) => {
    ev.preventDefault()
    if (!validate()) return
    setLoading(true)
    try {
      await register(form)
      toast.success('Account created! Welcome to PayFlow 🎉')
      navigate('/dashboard')
    } catch (err: any) {
      const msg = err.response?.data?.message ?? 'Registration failed'
      toast.error(msg)
    } finally {
      setLoading(false)
    }
  }

  const field = (
    label: string,
    key: keyof FormState,
    type = 'text',
    placeholder = '',
    extra?: React.ReactNode,
  ) => (
    <div>
      <label className="label">{label}</label>
      <div className="relative">
        <input
          type={key === 'password' ? (showPw ? 'text' : 'password') : type}
          value={form[key]}
          onChange={set(key)}
          className={`${errors[key] ? 'input-error' : 'input'} ${extra ? 'pr-10' : ''}`}
          placeholder={placeholder}
        />
        {extra}
      </div>
      {errors[key] && <p className="mt-1 text-xs text-danger-600">{errors[key]}</p>}
    </div>
  )

  return (
    <div className="min-h-screen bg-gradient-to-br from-primary-50 via-white to-blue-50
                    flex items-center justify-center p-4">
      <div className="w-full max-w-md animate-slide-up">
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-12 h-12
                          bg-primary-600 rounded-2xl shadow-lg mb-4">
            <Zap className="w-6 h-6 text-white" />
          </div>
          <h1 className="text-2xl font-bold text-gray-900">Create your account</h1>
          <p className="text-sm text-gray-500 mt-1">Join PayFlow — it's free</p>
        </div>

        <div className="card p-8">
          <form onSubmit={handleSubmit} className="space-y-4" noValidate>
            <div className="grid grid-cols-2 gap-3">
              {field('First name', 'firstName', 'text', 'John')}
              {field('Last name',  'lastName',  'text', 'Doe')}
            </div>
            {field('Username', 'username', 'text', 'john_doe')}
            {field('Email',    'email',    'email', 'john@example.com')}
            {field('Password', 'password', 'password', '••••••••',
              <button
                type="button"
                onClick={() => setShowPw(v => !v)}
                className="absolute inset-y-0 right-0 flex items-center pr-3
                           text-gray-400 hover:text-gray-600"
              >
                {showPw ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
              </button>
            )}

            <button
              type="submit"
              disabled={loading}
              className="btn-primary w-full justify-center py-2.5 text-base mt-2"
            >
              {loading
                ? <><Loader2 className="w-4 h-4 animate-spin" /> Creating account…</>
                : 'Create account'
              }
            </button>
          </form>

          <p className="mt-6 text-center text-sm text-gray-500">
            Already have an account?{' '}
            <Link to="/login" className="font-semibold text-primary-600 hover:text-primary-700">
              Sign in
            </Link>
          </p>
        </div>
      </div>
    </div>
  )
}
