import { useEffect, useState } from 'react'
import { orgApi } from '../api/client'
import { useAuth } from '../auth/AuthContext'

export default function OrganizationsPage() {
  const { hasRole, user } = useAuth()
  const isAdmin = hasRole('ADMIN')
  const [agencies, setAgencies] = useState([])
  const [merchants, setMerchants] = useState([])
  const [tab, setTab] = useState(isAdmin ? 'agencies' : 'merchants')
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [agencyForm, setAgencyForm] = useState({ code: '', name: '' })
  const [merchantForm, setMerchantForm] = useState({
    agencyId: user?.agencyId || '',
    code: '',
    name: '',
  })

  async function load() {
    const tasks = [orgApi.listMerchants()]
    if (isAdmin) tasks.unshift(orgApi.listAgencies())
    const results = await Promise.all(tasks)
    if (isAdmin) {
      setAgencies(results[0] || [])
      setMerchants(results[1] || [])
    } else {
      setMerchants(results[0] || [])
    }
  }

  useEffect(() => {
    load().catch((err) => setError(err.message))
  }, [])

  async function createAgency(e) {
    e.preventDefault()
    setError('')
    setMessage('')
    try {
      await orgApi.createAgency(agencyForm)
      setAgencyForm({ code: '', name: '' })
      setMessage('대행사가 등록되었습니다.')
      await load()
    } catch (err) {
      setError(err.message)
    }
  }

  async function createMerchant(e) {
    e.preventDefault()
    setError('')
    setMessage('')
    try {
      await orgApi.createMerchant({
        agencyId: merchantForm.agencyId ? Number(merchantForm.agencyId) : undefined,
        code: merchantForm.code,
        name: merchantForm.name,
      })
      setMerchantForm((prev) => ({ ...prev, code: '', name: '' }))
      setMessage('가맹점이 등록되었습니다.')
      await load()
    } catch (err) {
      setError(err.message)
    }
  }

  async function toggleAgency(agency) {
    try {
      await orgApi.updateAgency(agency.id, { isActive: !agency.active })
      await load()
    } catch (err) {
      setError(err.message)
    }
  }

  async function toggleMerchant(merchant) {
    try {
      await orgApi.updateMerchant(merchant.id, { isActive: !merchant.active })
      await load()
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <section className="stack">
      <div className="section-head">
        <h2>조직</h2>
        <p>대행사·가맹점 등록 및 활성 상태 관리</p>
      </div>

      {error && <p className="error-text">{error}</p>}
      {message && <p className="ok-text">{message}</p>}

      <div className="actions">
        {isAdmin && (
          <button
            type="button"
            className={`btn ${tab === 'agencies' ? 'btn-primary' : 'btn-ghost'}`}
            onClick={() => setTab('agencies')}
          >
            대행사
          </button>
        )}
        <button
          type="button"
          className={`btn ${tab === 'merchants' ? 'btn-primary' : 'btn-ghost'}`}
          onClick={() => setTab('merchants')}
        >
          가맹점
        </button>
      </div>

      {tab === 'agencies' && isAdmin && (
        <>
          <form className="panel form-row" onSubmit={createAgency}>
            <label>
              코드
              <input
                value={agencyForm.code}
                onChange={(e) => setAgencyForm({ ...agencyForm, code: e.target.value })}
                required
              />
            </label>
            <label>
              이름
              <input
                value={agencyForm.name}
                onChange={(e) => setAgencyForm({ ...agencyForm, name: e.target.value })}
                required
              />
            </label>
            <button className="btn btn-primary" type="submit">
              대행사 추가
            </button>
          </form>
          <div className="panel">
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>코드</th>
                  <th>이름</th>
                  <th>활성</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {agencies.map((a) => (
                  <tr key={a.id}>
                    <td>{a.id}</td>
                    <td>{a.code}</td>
                    <td>{a.name}</td>
                    <td>{a.active ? 'Y' : 'N'}</td>
                    <td>
                      <button className="btn btn-ghost" type="button" onClick={() => toggleAgency(a)}>
                        {a.active ? '비활성' : '활성'}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}

      {tab === 'merchants' && (
        <>
          <form className="panel form-row" onSubmit={createMerchant}>
            {isAdmin && (
              <label>
                대행사 ID
                <input
                  value={merchantForm.agencyId}
                  onChange={(e) => setMerchantForm({ ...merchantForm, agencyId: e.target.value })}
                  required
                />
              </label>
            )}
            <label>
              코드
              <input
                value={merchantForm.code}
                onChange={(e) => setMerchantForm({ ...merchantForm, code: e.target.value })}
                required
              />
            </label>
            <label>
              이름
              <input
                value={merchantForm.name}
                onChange={(e) => setMerchantForm({ ...merchantForm, name: e.target.value })}
                required
              />
            </label>
            <button className="btn btn-primary" type="submit">
              가맹점 추가
            </button>
          </form>
          <div className="panel">
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>대행사</th>
                  <th>코드</th>
                  <th>이름</th>
                  <th>활성</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {merchants.map((m) => (
                  <tr key={m.id}>
                    <td>{m.id}</td>
                    <td>
                      {m.agencyCode} (#{m.agencyId})
                    </td>
                    <td>{m.code}</td>
                    <td>{m.name}</td>
                    <td>{m.active ? 'Y' : 'N'}</td>
                    <td>
                      <button
                        className="btn btn-ghost"
                        type="button"
                        onClick={() => toggleMerchant(m)}
                      >
                        {m.active ? '비활성' : '활성'}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
    </section>
  )
}
