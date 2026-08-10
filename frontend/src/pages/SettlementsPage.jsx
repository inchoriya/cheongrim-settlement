import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { settlementsApi } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import StatusBadge from '../components/StatusBadge'
import { formatDateTime, formatWon, fromDateTimeLocal, toDateTimeLocal } from '../utils/format'

export default function SettlementsPage() {
  const { hasRole } = useAuth()
  const [items, setItems] = useState([])
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [batch, setBatch] = useState({
    periodStart: '2026-08-01T00:00',
    periodEnd: '2026-08-08T00:00',
    agencyId: '',
  })

  async function load() {
    const page = await settlementsApi.list({ size: 50 })
    setItems(page.content || [])
  }

  useEffect(() => {
    load().catch((err) => setError(err.message))
  }, [])

  async function runBatch(e) {
    e.preventDefault()
    setError('')
    setMessage('')
    try {
      const result = await settlementsApi.batch({
        periodStart: fromDateTimeLocal(batch.periodStart),
        periodEnd: fromDateTimeLocal(batch.periodEnd),
        agencyId: batch.agencyId ? Number(batch.agencyId) : null,
      })
      setMessage(
        `배치 완료: 주문 ${result.processedOrderCount}건 → 정산 ${result.createdSettlementCount}건`,
      )
      await load()
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <section className="stack">
      <div className="section-head">
        <h2>정산</h2>
        <p>정산 배치 실행과 결과 조회</p>
      </div>

      {error && <p className="error-text">{error}</p>}
      {message && <p className="ok-text">{message}</p>}

      {hasRole('ADMIN') && (
        <form className="panel form-row" onSubmit={runBatch}>
          <label>
            기간 시작
            <input
              type="datetime-local"
              value={toDateTimeLocal(batch.periodStart)}
              onChange={(e) => setBatch({ ...batch, periodStart: e.target.value })}
            />
          </label>
          <label>
            기간 종료
            <input
              type="datetime-local"
              value={toDateTimeLocal(batch.periodEnd)}
              onChange={(e) => setBatch({ ...batch, periodEnd: e.target.value })}
            />
          </label>
          <label>
            대행사 ID (선택)
            <input
              value={batch.agencyId}
              onChange={(e) => setBatch({ ...batch, agencyId: e.target.value })}
            />
          </label>
          <button className="btn btn-primary" type="submit">
            배치 실행
          </button>
        </form>
      )}

      <div className="panel">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>가맹점</th>
              <th>기간</th>
              <th>주문수</th>
              <th>가맹점 정산액</th>
              <th>상태</th>
            </tr>
          </thead>
          <tbody>
            {items.map((s) => (
              <tr key={s.id}>
                <td>
                  <Link to={`/settlements/${s.id}`}>#{s.id}</Link>
                </td>
                <td>{s.merchantName}</td>
                <td>
                  {formatDateTime(s.periodStart)} ~ {formatDateTime(s.periodEnd)}
                </td>
                <td>{s.orderCount}</td>
                <td>{formatWon(s.totalMerchantSettlementAmount)}</td>
                <td>
                  <StatusBadge status={s.status} />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  )
}
