import { useEffect, useState } from 'react'
import { policiesApi } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { formatDateTime, formatWon, fromDateTimeLocal, toDateTimeLocal } from '../utils/format'

export default function PoliciesPage() {
  const { hasRole } = useAuth()
  const [items, setItems] = useState([])
  const [error, setError] = useState('')
  const [form, setForm] = useState({
    agencyId: '',
    name: '신규 정책',
    platformFeeBps: '500',
    agencyFeeBps: '1000',
    riderFee: '3000',
    effectiveFrom: '2026-01-01T00:00',
    effectiveTo: '',
  })

  async function load() {
    setItems(await policiesApi.list())
  }

  useEffect(() => {
    load().catch((err) => setError(err.message))
  }, [])

  async function onCreate(e) {
    e.preventDefault()
    setError('')
    try {
      await policiesApi.create({
        agencyId: form.agencyId ? Number(form.agencyId) : null,
        name: form.name,
        platformFeeBps: Number(form.platformFeeBps),
        agencyFeeBps: Number(form.agencyFeeBps),
        riderFee: Number(form.riderFee),
        effectiveFrom: fromDateTimeLocal(form.effectiveFrom),
        effectiveTo: form.effectiveTo ? fromDateTimeLocal(form.effectiveTo) : null,
      })
      await load()
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <section className="stack">
      <div className="section-head">
        <h2>수수료 정책</h2>
        <p>수수료 정책 (bps, 건당 라이더비)</p>
      </div>

      {error && <p className="error-text">{error}</p>}

      {hasRole('ADMIN') && (
        <form className="panel form-row" onSubmit={onCreate}>
          <label>
            대행사 ID (빈값=전역)
            <input
              value={form.agencyId}
              onChange={(e) => setForm({ ...form, agencyId: e.target.value })}
            />
          </label>
          <label>
            정책명
            <input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
          </label>
          <label>
            플랫폼 수수료(bps)
            <input
              value={form.platformFeeBps}
              onChange={(e) => setForm({ ...form, platformFeeBps: e.target.value })}
            />
          </label>
          <label>
            대행사 수수료(bps)
            <input
              value={form.agencyFeeBps}
              onChange={(e) => setForm({ ...form, agencyFeeBps: e.target.value })}
            />
          </label>
          <label>
            라이더비
            <input
              value={form.riderFee}
              onChange={(e) => setForm({ ...form, riderFee: e.target.value })}
            />
          </label>
          <label>
            적용 시작
            <input
              type="datetime-local"
              value={toDateTimeLocal(form.effectiveFrom)}
              onChange={(e) => setForm({ ...form, effectiveFrom: e.target.value })}
            />
          </label>
          <button className="btn btn-primary" type="submit">
            정책 추가
          </button>
        </form>
      )}

      <div className="panel">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>이름</th>
              <th>대행사</th>
              <th>플랫폼</th>
              <th>대행 수수료</th>
              <th>라이더</th>
              <th>유효기간</th>
            </tr>
          </thead>
          <tbody>
            {items.map((p) => (
              <tr key={p.id}>
                <td>{p.id}</td>
                <td>{p.name}</td>
                <td>{p.agencyId ?? '전역'}</td>
                <td>{p.platformFeeBps} bps</td>
                <td>{p.agencyFeeBps} bps</td>
                <td>{formatWon(p.riderFee)}</td>
                <td>
                  {formatDateTime(p.effectiveFrom)} ~ {p.effectiveTo ? formatDateTime(p.effectiveTo) : '무기한'}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  )
}
