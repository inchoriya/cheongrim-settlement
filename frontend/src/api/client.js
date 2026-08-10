const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1'

function getToken() {
  return localStorage.getItem('settlehub_token')
}

export async function api(path, options = {}) {
  const headers = new Headers(options.headers || {})
  if (!(options.body instanceof FormData) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }
  const token = getToken()
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers,
  })

  const payload = await response.json().catch(() => null)
  if (!response.ok || payload?.success === false) {
    const error = new Error(payload?.error?.message || `요청 실패 (${response.status})`)
    error.code = payload?.error?.code || 'HTTP_ERROR'
    error.status = response.status
    throw error
  }
  return payload.data
}

export const authApi = {
  login: (email, password) =>
    api('/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) }),
  me: () => api('/auth/me'),
}

export const dashboardApi = {
  summary: () => api('/dashboard/summary'),
}

export const ordersApi = {
  list: (params = {}) => {
    const qs = new URLSearchParams()
    Object.entries(params).forEach(([k, v]) => {
      if (v !== undefined && v !== null && v !== '') qs.set(k, v)
    })
    const query = qs.toString()
    return api(`/orders${query ? `?${query}` : ''}`)
  },
  create: (body) => api('/orders', { method: 'POST', body: JSON.stringify(body) }),
  upload: (file, agencyId) => {
    const form = new FormData()
    form.append('file', file)
    const qs = agencyId ? `?agencyId=${agencyId}` : ''
    return api(`/orders/upload${qs}`, { method: 'POST', body: form })
  },
  cancel: (id) => api(`/orders/${id}/cancel`, { method: 'PATCH' }),
}

export const settlementsApi = {
  list: (params = {}) => {
    const qs = new URLSearchParams()
    Object.entries(params).forEach(([k, v]) => {
      if (v !== undefined && v !== null && v !== '') qs.set(k, v)
    })
    const query = qs.toString()
    return api(`/settlements${query ? `?${query}` : ''}`)
  },
  get: (id) => api(`/settlements/${id}`),
  batch: (body) => api('/settlements/batch', { method: 'POST', body: JSON.stringify(body) }),
  hold: (id, reason) =>
    api(`/settlements/${id}/hold`, { method: 'POST', body: JSON.stringify({ reason }) }),
  confirm: (id) => api(`/settlements/${id}/confirm`, { method: 'POST' }),
  ready: (id) => api(`/settlements/${id}/ready-for-payout`, { method: 'POST' }),
}

export const payoutsApi = {
  list: (params = {}) => {
    const qs = new URLSearchParams()
    Object.entries(params).forEach(([k, v]) => {
      if (v !== undefined && v !== null && v !== '') qs.set(k, v)
    })
    const query = qs.toString()
    return api(`/payouts${query ? `?${query}` : ''}`)
  },
  create: (settlementId, forceFail = false) =>
    api('/payouts', {
      method: 'POST',
      body: JSON.stringify({ settlementId, forceFail }),
    }),
}

export const policiesApi = {
  list: () => api('/policies'),
  create: (body) => api('/policies', { method: 'POST', body: JSON.stringify(body) }),
}

export const orgApi = {
  listAgencies: (activeOnly = false) => api(`/agencies?activeOnly=${activeOnly}`),
  createAgency: (body) => api('/agencies', { method: 'POST', body: JSON.stringify(body) }),
  updateAgency: (id, body) => api(`/agencies/${id}`, { method: 'PATCH', body: JSON.stringify(body) }),
  listMerchants: () => api('/merchants'),
  createMerchant: (body) => api('/merchants', { method: 'POST', body: JSON.stringify(body) }),
  updateMerchant: (id, body) =>
    api(`/merchants/${id}`, { method: 'PATCH', body: JSON.stringify(body) }),
}

export const auditApi = {
  list: (params = {}) => {
    const qs = new URLSearchParams()
    Object.entries(params).forEach(([k, v]) => {
      if (v !== undefined && v !== null && v !== '') qs.set(k, v)
    })
    const query = qs.toString()
    return api(`/audit-logs${query ? `?${query}` : ''}`)
  },
}
