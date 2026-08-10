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
      <form className="login-panel" onSubmit={onSubmit}>
        <p className="eyebrow">청림인베스트</p>
        <h1>정산관리 로그인</h1>
        <p className="muted">데모 계정 버튼으로 이메일·비밀번호를 채울 수 있습니다.</p>

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
  )
}
