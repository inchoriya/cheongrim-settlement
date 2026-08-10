import { useEffect, useState } from 'react'
import { auditApi } from '../api/client'
import { formatDateTime } from '../utils/format'

export default function AuditLogsPage() {
  const [items, setItems] = useState([])
  const [error, setError] = useState('')
  const [filters, setFilters] = useState({
    entityType: '',
    entityId: '',
  })
  const [selected, setSelected] = useState(null)

  async function load() {
    const page = await auditApi.list({
      entityType: filters.entityType || undefined,
      entityId: filters.entityId || undefined,
      size: 50,
    })
    setItems(page.content || [])
  }

  useEffect(() => {
    load().catch((err) => setError(err.message))
  }, [])

  async function onSearch(e) {
    e.preventDefault()
    setError('')
    try {
      await load()
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <section className="stack">
      <div className="section-head">
        <h2>감사 로그</h2>
        <p>정산 확정·지급 등 상태 변경 이력</p>
      </div>

      {error && <p className="error-text">{error}</p>}

      <form className="panel form-row" onSubmit={onSearch}>
        <label>
          대상 유형
          <input
            placeholder="Settlement"
            value={filters.entityType}
            onChange={(e) => setFilters({ ...filters, entityType: e.target.value })}
          />
        </label>
        <label>
          대상 ID
          <input
            value={filters.entityId}
            onChange={(e) => setFilters({ ...filters, entityId: e.target.value })}
          />
        </label>
        <button className="btn btn-primary" type="submit">
          조회
        </button>
      </form>

      <div className="panel">
        <table>
          <thead>
            <tr>
              <th>시각</th>
              <th>수행자</th>
              <th>액션</th>
              <th>대상</th>
              <th>사유</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {items.length === 0 && (
              <tr>
                <td colSpan={6} className="muted">
                  감사 로그가 없습니다. 정산 확정/지급을 실행하면 기록됩니다.
                </td>
              </tr>
            )}
            {items.map((log) => (
              <tr key={log.id}>
                <td>{formatDateTime(log.createdAt)}</td>
                <td>{log.actorEmail || '-'}</td>
                <td>{log.action}</td>
                <td>
                  {log.entityType} #{log.entityId}
                </td>
                <td>{log.reason || '-'}</td>
                <td>
                  <button className="btn btn-ghost" type="button" onClick={() => setSelected(log)}>
                    상세
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {selected && (
        <div className="panel stack-sm">
          <div className="panel-head">
            <h3>
              #{selected.id} {selected.action}
            </h3>
            <button className="btn btn-ghost" type="button" onClick={() => setSelected(null)}>
              닫기
            </button>
          </div>
          <p className="muted">
            변경 전: {selected.beforeJson || '-'} / 변경 후: {selected.afterJson || '-'}
          </p>
        </div>
      )}
    </section>
  )
}
