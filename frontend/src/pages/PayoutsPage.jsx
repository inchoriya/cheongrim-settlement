import { useEffect, useState } from 'react'
import { payoutsApi, settlementsApi } from '../api/client'
import StatusBadge from '../components/StatusBadge'
import { formatWon } from '../utils/format'

export default function PayoutsPage() {
  const [payouts, setPayouts] = useState([])
  const [readyItems, setReadyItems] = useState([])
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [forceFail, setForceFail] = useState(false)

  async function load() {
    const [p, ready] = await Promise.all([
      payoutsApi.list(),
      settlementsApi.list({ status: 'READY_FOR_PAYOUT', size: 50 }),
    ])
    setPayouts(p || [])
    setReadyItems(ready.content || [])
  }

  useEffect(() => {
    load().catch((err) => setError(err.message))
  }, [])

  async function pay(settlementId) {
    setError('')
    setMessage('')
    try {
      const result = await payoutsApi.create(settlementId, forceFail)
      setMessage(`지급 결과: ${result.status} ${result.pgTransactionId || result.failureReason || ''}`)
      await load()
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <section className="stack">
      <div className="section-head">
        <h2>지급</h2>
        <p>모의 PG 지급 실행 및 이력</p>
      </div>

      {error && <p className="error-text">{error}</p>}
      {message && <p className="ok-text">{message}</p>}

      <div className="panel">
        <div className="panel-head">
          <h3>지급 대기 정산</h3>
          <label className="inline">
            <input
              type="checkbox"
              checked={forceFail}
              onChange={(e) => setForceFail(e.target.checked)}
            />
            실패 강제 (데모)
          </label>
        </div>
        <table>
          <thead>
            <tr>
              <th>정산</th>
              <th>가맹점</th>
              <th>금액</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {readyItems.map((s) => (
              <tr key={s.id}>
                <td>#{s.id}</td>
                <td>{s.merchantName}</td>
                <td>{formatWon(s.totalMerchantSettlementAmount)}</td>
                <td>
                  <button className="btn btn-primary" type="button" onClick={() => pay(s.id)}>
                    지급 실행
                  </button>
                </td>
              </tr>
            ))}
            {readyItems.length === 0 && (
              <tr>
                <td colSpan={4} className="muted">
                  지급 대기 정산이 없습니다.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      <div className="panel">
        <h3>지급 이력</h3>
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>정산</th>
              <th>금액</th>
              <th>상태</th>
              <th>거래번호</th>
            </tr>
          </thead>
          <tbody>
            {payouts.map((p) => (
              <tr key={p.payoutId}>
                <td>{p.payoutId}</td>
                <td>#{p.settlementId}</td>
                <td>{formatWon(p.amount)}</td>
                <td>
                  <StatusBadge status={p.status} />
                </td>
                <td>{p.pgTransactionId || p.failureReason || '-'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  )
}
