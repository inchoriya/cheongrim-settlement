import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { dashboardApi, settlementsApi } from '../api/client'
import StatusBadge from '../components/StatusBadge'
import { formatWon } from '../utils/format'

export default function DashboardPage() {
  const [summary, setSummary] = useState(null)
  const [recent, setRecent] = useState([])
  const [error, setError] = useState('')

  useEffect(() => {
    Promise.all([dashboardApi.summary(), settlementsApi.list({ size: 5 })])
      .then(([s, page]) => {
        setSummary(s)
        setRecent(page.content || [])
      })
      .catch((err) => setError(err.message))
  }, [])

  if (error) return <p className="error-text">{error}</p>
  if (!summary) return <p className="muted">불러오는 중...</p>

  const cards = [
    { label: '주문 수', value: summary.orderCount.toLocaleString('ko-KR') },
    { label: '주문 금액', value: formatWon(summary.orderAmountSum) },
    { label: '정산 대기', value: summary.settlementPendingCount },
    { label: '보류', value: summary.heldCount },
    { label: '지급 대기', value: summary.readyForPayoutCount },
    { label: '지급 완료액', value: formatWon(summary.paidAmountSum) },
  ]

  return (
    <section className="stack">
      <div className="section-head">
        <h2>대시보드</h2>
        <p>현재 권한 범위 기준 운영 요약입니다.</p>
      </div>

      <div className="stat-grid">
        {cards.map((c) => (
          <article key={c.label} className="stat">
            <span>{c.label}</span>
            <strong>{c.value}</strong>
          </article>
        ))}
      </div>

      <div className="panel">
        <div className="panel-head">
          <h3>최근 정산</h3>
          <Link to="/settlements">전체 보기</Link>
        </div>
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>가맹점</th>
              <th>가맹점 정산액</th>
              <th>상태</th>
            </tr>
          </thead>
          <tbody>
            {recent.length === 0 && (
              <tr>
                <td colSpan={4} className="muted">
                  정산 데이터가 없습니다.
                </td>
              </tr>
            )}
            {recent.map((s) => (
              <tr key={s.id}>
                <td>
                  <Link to={`/settlements/${s.id}`}>#{s.id}</Link>
                </td>
                <td>{s.merchantName}</td>
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
