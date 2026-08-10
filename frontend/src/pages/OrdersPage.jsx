import { useEffect, useState } from 'react'
import { ordersApi } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import StatusBadge from '../components/StatusBadge'
import { formatDateTime, formatWon, fromDateTimeLocal, toDateTimeLocal } from '../utils/format'

export default function OrdersPage() {
  const { hasRole, user } = useAuth()
  const canWrite = hasRole('ADMIN', 'AGENCY')
  const [orders, setOrders] = useState([])
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [form, setForm] = useState({
    externalOrderId: '',
    merchantId: '',
    agencyId: user?.agencyId || '',
    orderAmount: '20000',
    deliveryTip: '0',
    orderedAt: '2026-08-01T12:30',
  })

  async function load() {
    try {
      const page = await ordersApi.list({ size: 50 })
      setOrders(page.content || [])
    } catch (err) {
      setError(err.message)
    }
  }

  useEffect(() => {
    load()
  }, [])

  async function onCreate(e) {
    e.preventDefault()
    setMessage('')
    setError('')
    try {
      await ordersApi.create({
        externalOrderId: form.externalOrderId,
        merchantId: Number(form.merchantId),
        agencyId: form.agencyId ? Number(form.agencyId) : undefined,
        orderAmount: Number(form.orderAmount),
        deliveryTip: Number(form.deliveryTip || 0),
        orderedAt: fromDateTimeLocal(form.orderedAt),
      })
      setMessage('주문이 등록되었습니다.')
      setForm((prev) => ({ ...prev, externalOrderId: '' }))
      await load()
    } catch (err) {
      setError(err.message)
    }
  }

  async function onUpload(e) {
    const file = e.target.files?.[0]
    if (!file) return
    setMessage('')
    setError('')
    try {
      const agencyId = hasRole('ADMIN') ? Number(form.agencyId || 0) || undefined : undefined
      const result = await ordersApi.upload(file, agencyId)
      setMessage(`업로드 완료: 성공 ${result.successCount} / 실패 ${result.failureCount}`)
      await load()
    } catch (err) {
      setError(err.message)
    } finally {
      e.target.value = ''
    }
  }

  async function onCancel(id) {
    setError('')
    try {
      await ordersApi.cancel(id)
      await load()
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <section className="stack">
      <div className="section-head">
        <h2>주문</h2>
        <p>주문 조회와 CSV 업로드를 처리합니다.</p>
      </div>

      {error && <p className="error-text">{error}</p>}
      {message && <p className="ok-text">{message}</p>}

      {canWrite && (
        <div className="panel grid-2">
          <form className="stack-sm" onSubmit={onCreate}>
            <h3>단건 등록</h3>
            {hasRole('ADMIN') && (
              <label>
                대행사 ID
                <input
                  value={form.agencyId}
                  onChange={(e) => setForm({ ...form, agencyId: e.target.value })}
                  required
                />
              </label>
            )}
            <label>
              외부 주문번호
              <input
                value={form.externalOrderId}
                onChange={(e) => setForm({ ...form, externalOrderId: e.target.value })}
                required
              />
            </label>
            <label>
              가맹점 ID
              <input
                value={form.merchantId}
                onChange={(e) => setForm({ ...form, merchantId: e.target.value })}
                required
              />
            </label>
            <label>
              주문 금액
              <input
                value={form.orderAmount}
                onChange={(e) => setForm({ ...form, orderAmount: e.target.value })}
                required
              />
            </label>
            <label>
              주문 시각
              <input
                type="datetime-local"
                value={toDateTimeLocal(form.orderedAt)}
                onChange={(e) => setForm({ ...form, orderedAt: e.target.value })}
                required
              />
            </label>
            <button className="btn btn-primary" type="submit">
              등록
            </button>
          </form>

          <div className="stack-sm">
            <h3>CSV 업로드</h3>
            <p className="muted">
              헤더: externalOrderId,merchantCode,orderAmount,deliveryTip,orderedAt,status
            </p>
            {hasRole('ADMIN') && (
              <label>
                대행사 ID
                <input
                  value={form.agencyId}
                  onChange={(e) => setForm({ ...form, agencyId: e.target.value })}
                />
              </label>
            )}
            <input type="file" accept=".csv,text/csv" onChange={onUpload} />
          </div>
        </div>
      )}

      <div className="panel">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>외부 주문번호</th>
              <th>가맹점</th>
              <th>금액</th>
              <th>주문시각</th>
              <th>상태</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {orders.map((o) => (
              <tr key={o.id}>
                <td>{o.id}</td>
                <td>{o.externalOrderId}</td>
                <td>{o.merchantName}</td>
                <td>{formatWon(o.orderAmount)}</td>
                <td>{formatDateTime(o.orderedAt)}</td>
                <td>
                  <StatusBadge status={o.status} />
                </td>
                <td>
                  {canWrite && o.status === 'CREATED' && !o.settlementLocked && (
                    <button className="btn btn-ghost" type="button" onClick={() => onCancel(o.id)}>
                      취소
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  )
}
