import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import { ProtectedRoute, GuestRoute } from './components/common/ProtectedRoute'
import { AppLayout } from './components/layout/AppLayout'

// Lazy-load pages
import { lazy, Suspense } from 'react'

const LoginPage       = lazy(() => import('./pages/LoginPage'))
const RegisterPage    = lazy(() => import('./pages/RegisterPage'))
const DashboardPage   = lazy(() => import('./pages/DashboardPage'))
const PaymentsPage    = lazy(() => import('./pages/PaymentsPage'))
const TransactionsPage= lazy(() => import('./pages/TransactionsPage'))
const AnalyticsPage   = lazy(() => import('./pages/AnalyticsPage'))
const ProfilePage     = lazy(() => import('./pages/ProfilePage'))

function PageLoader() {
  return (
    <div className="min-h-screen flex items-center justify-center">
      <div className="w-8 h-8 border-4 border-primary-600 border-t-transparent
                      rounded-full animate-spin" />
    </div>
  )
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Suspense fallback={<PageLoader />}>
          <Routes>
            {/* Public */}
            <Route element={<GuestRoute />}>
              <Route path="/login"    element={<LoginPage />} />
              <Route path="/register" element={<RegisterPage />} />
            </Route>

            {/* Protected */}
            <Route element={<ProtectedRoute />}>
              <Route element={<AppLayout />}>
                <Route path="/dashboard"    element={<DashboardPage />} />
                <Route path="/payments"     element={<PaymentsPage />} />
                <Route path="/transactions" element={<TransactionsPage />} />
                <Route path="/analytics"    element={<AnalyticsPage />} />
                <Route path="/profile"      element={<ProfilePage />} />
              </Route>
            </Route>

            {/* Fallback */}
            <Route path="*" element={<Navigate to="/dashboard" replace />} />
          </Routes>
        </Suspense>
      </BrowserRouter>
    </AuthProvider>
  )
}
