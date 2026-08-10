import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { settlementsApi } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import StatusBadge from '../components/StatusBadge'
import { formatWon } from '../utils/format'

export default function SettlementDetailPage() {
  const { id } = useParams()
  const { hasRole } = useAuth()
  const [settlement, setSettlement] = useState(null)
  const [error, setError] = useState('')
  const [holdReason, setHoldReason] = useState('수수료 정책 재확인 필요')

  async function load() {
    setSettlement(await settlementsApi.get(id))
  }

  useEffect(() => {
    load().catch((err) => setError(err.message))
  }, [id])

  async function act(action) {
    setError('')
    try {
      if (action === 'hold') await settlementsApi.hold(id, holdReason)
      if (action === 'confirm') await settlementsApi.confirm(id)
      if (action === 'ready') await settlementsApi.ready(id)
      await load()
    } catch (err) {
      setError(err.message)
    }
  }

  if (error && !settlement) return <p className="error-text">{error}</p>
  if (!settlement) return <p className="muted">불러오는 중...</p>

  const actionableStatuses = ['CALCULATED', 'HELD', 'CONFIRMED']
  const showActions = hasRole('ADMIN') && actionableStatuses.includes(settlement.status)

  return (
    <section className="stack">
      <div className="section-head">
        <p>
          <Link to="/settlements">← 정산 목록</Link>
        </p>
        <h2>
          정산 #{settlement.id} <StatusBadge status={settlement.status} />
        </h2>
        <p>
          {settlement.merchantName} · 가맹점 정산 {formatWon(settlement.totalMerchantSettlementAmount)}
        </p>
      </div>

      {error && <p className="error-text">{error}</p>}

      {showActions && (
        <div className="panel actions">
          {settlement.status === 'CALCULATED' && (
            <>
              <input value={holdReason} onChange={(e) => setHoldReason(e.target.value)} />
              <button className="btn btn-ghost" type="button" onClick={() => act('hold')}>
                보류
              </button>
              <button className="btn btn-primary" type="button" onClick={() => act('confirm')}>
                확정
              </button>
            </>
          )}
          {settlement.status === 'HELD' && (
            <button className="btn btn-primary" type="button" onClick={() => act('confirm')}>
              보류 해제 후 확정
            </button>
          )}
          {settlement.status === 'CONFIRMED' && (
            <button className="btn btn-primary" type="button" onClick={() => act('ready')}>
              지급 대기로
            </button>
          )}
        </div>
      )}

      <div className="stat-grid">
        <article className="stat">
          <span>주문금액 합</span>
          <strong>{formatWon(settlement.totalOrderAmount)}</strong>
        </article>
        <article className="stat">
          <span>플랫폼 수수료</span>
          <strong>{formatWon(settlement.totalPlatformFeeAmount)}</strong>
        </article>
        <article className="stat">
          <span>대행사 정산</span>
          <strong>{formatWon(settlement.totalAgencySettlementAmount)}</strong>
        </article>
        <article className="stat">
          <span>라이더비</span>
          <strong>{formatWon(settlement.totalRiderFeeAmount)}</strong>
        </article>
      </div>

      <div className="panel">
        <table>
          <thead>
            <tr>
              <th>주문</th>
              <th>주문금액</th>
              <th>플랫폼</th>
              <th>대행</th>
              <th>라이더</th>
              <th>가맹점</th>
              <th>이상</th>
            </tr>
          </thead>
          <tbody>
            {(settlement.lines || []).map((line) => (
              <tr key={line.id}>
                <td>{line.externalOrderId}</td>
                <td>{formatWon(line.orderAmount)}</td>
                <td>{formatWon(line.platformFeeAmount)}</td>
                <td>{formatWon(line.agencyFeeAmount)}</td>
                <td>{formatWon(line.riderFeeAmount)}</td>
                <td>{formatWon(line.merchantSettlementAmount)}</td>
                <td>{line.anomalyFlag || '-'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  )
}
