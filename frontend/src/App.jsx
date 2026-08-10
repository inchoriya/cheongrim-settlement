import { Navigate, Route, Routes } from 'react-router-dom'
import { useAuth } from './auth/AuthContext'
import Layout from './components/Layout'
import AuditLogsPage from './pages/AuditLogsPage'
import DashboardPage from './pages/DashboardPage'
import LoginPage from './pages/LoginPage'
import OrdersPage from './pages/OrdersPage'
import OrganizationsPage from './pages/OrganizationsPage'
import PayoutsPage from './pages/PayoutsPage'
import PoliciesPage from './pages/PoliciesPage'
import SettlementDetailPage from './pages/SettlementDetailPage'
import SettlementsPage from './pages/SettlementsPage'

function Protected({ children, roles }) {
  const { token, loading, hasRole } = useAuth()
  if (loading) return <p className="muted page-center">인증 확인 중...</p>
  if (!token) return <Navigate to="/login" replace />
  if (roles && !hasRole(...roles)) return <p className="error-text page-center">접근 권한이 없습니다.</p>
  return children
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        path="/"
        element={
          <Protected>
            <Layout />
          </Protected>
        }
      >
        <Route index element={<DashboardPage />} />
        <Route path="orders" element={<OrdersPage />} />
        <Route path="settlements" element={<SettlementsPage />} />
        <Route path="settlements/:id" element={<SettlementDetailPage />} />
        <Route
          path="payouts"
          element={
            <Protected roles={['ADMIN']}>
              <PayoutsPage />
            </Protected>
          }
        />
        <Route
          path="policies"
          element={
            <Protected roles={['ADMIN', 'AGENCY']}>
              <PoliciesPage />
            </Protected>
          }
        />
        <Route
          path="organizations"
          element={
            <Protected roles={['ADMIN', 'AGENCY']}>
              <OrganizationsPage />
            </Protected>
          }
        />
        <Route
          path="audit-logs"
          element={
            <Protected roles={['ADMIN']}>
              <AuditLogsPage />
            </Protected>
          }
        />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
