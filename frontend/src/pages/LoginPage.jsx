import { useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

const DEMO_PASSWORD = 'Demo1234!'
const DEMOS = [
  { email: 'admin@cheongrim.local', role: '관리자' },
  { email: 'agency@seoul.local', role: '대행사' },
  { email: 'merchant@kimbap.local', role: '가맹점' },
]

export default function LoginPage() {
  const { login, token } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [pending, setPending] = useState(false)

  if (token) return <Navigate to="/" replace />

  async function onSubmit(e) {
    e.preventDefault()
    setPending(true)
    setError('')
    try {
      await login(email, password)
      navigate('/')
    } catch (err) {
      setError(err.message || '로그인 실패')
    } finally {
      setPending(false)
    }
  }

  function fillDemo(demoEmail) {
    setEmail(demoEmail)
    setPassword(DEMO_PASSWORD)
  }

  return (
    <div className="login-page">
      <section className="login-visual" aria-hidden="false">
        <div className="login-copy">
          <p className="login-kicker">CHEONGRIM INVEST</p>
          <h1>배달대행 정산을 정확하게.</h1>
          <p>주문 수집부터 수수료 계산, 주간 정산, 지급까지 한곳에서 운영합니다.</p>
        </div>
        <div className="login-meta">
          <strong>청림인베스트 · 정산관리</strong>
          <span>관리자 · 대행사 · 가맹점 역할 기반 접근</span>
        </div>
      </section>

      <div className="login-form-wrap">
        <form className="login-panel" onSubmit={onSubmit}>
          <h2>로그인</h2>
          <p className="muted">데모 계정으로 역할별 화면을 바로 확인할 수 있습니다.</p>

          <label>
            이메일
            <input
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              type="email"
              autoComplete="username"
              required
            />
          </label>
          <label>
            비밀번호
            <input
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              type="password"
              autoComplete="current-password"
              required
            />
          </label>

          {error && <p className="error-text">{error}</p>}

          <button className="btn btn-primary" disabled={pending} type="submit">
            {pending ? '로그인 중...' : '로그인'}
          </button>

          <div className="demo-accounts">
            {DEMOS.map((d) => (
              <button
                key={d.email}
                type="button"
                className="btn btn-ghost"
                onClick={() => fillDemo(d.email)}
              >
                {d.role}
              </button>
            ))}
          </div>
        </form>
      </div>
    </div>
  )
}
