import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

const ROLE_LABEL = {
  ADMIN: '관리자',
  AGENCY: '대행사',
  MERCHANT: '가맹점',
}

const MENUS = [
  { to: '/', label: '대시보드', roles: ['ADMIN', 'AGENCY', 'MERCHANT'] },
  { to: '/orders', label: '주문', roles: ['ADMIN', 'AGENCY', 'MERCHANT'] },
  { to: '/settlements', label: '정산', roles: ['ADMIN', 'AGENCY', 'MERCHANT'] },
  { to: '/payouts', label: '지급', roles: ['ADMIN'] },
  { to: '/policies', label: '수수료 정책', roles: ['ADMIN', 'AGENCY'] },
  { to: '/organizations', label: '조직', roles: ['ADMIN', 'AGENCY'] },
  { to: '/audit-logs', label: '감사 로그', roles: ['ADMIN'] },
]

export default function Layout() {
  const { user, logout, hasRole } = useAuth()
  const menus = MENUS.filter((m) => hasRole(...m.roles))

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-mark">CI</span>
          <div>
            <strong>청림인베스트</strong>
            <p>정산관리 시스템</p>
          </div>
        </div>
        <nav>
          {menus.map((m) => (
            <NavLink key={m.to} to={m.to} end={m.to === '/'}>
              {m.label}
            </NavLink>
          ))}
        </nav>
      </aside>
      <div className="main">
        <header className="topbar">
          <div>
            <h1>운영 워크스페이스</h1>
            <p>주문 · 정산 · 지급을 한 흐름으로 관리합니다.</p>
          </div>
          <div className="user-chip">
            <div>
              <strong>{user?.name}</strong>
              <span>
                {ROLE_LABEL[user?.role] || user?.role} · {user?.email}
              </span>
            </div>
            <button type="button" className="btn btn-ghost" onClick={logout}>
              로그아웃
            </button>
          </div>
        </header>
        <main className="content">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
