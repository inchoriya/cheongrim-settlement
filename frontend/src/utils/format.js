export function formatWon(value) {
  if (value === null || value === undefined) return '-'
  return `${Number(value).toLocaleString('ko-KR')}원`
}

/** 화면 표시용: 2026.08.08 00:00 */
export function formatDateTime(value) {
  if (!value) return '-'
  const date = parseApiDate(value)
  if (!date) return String(value)

  const yyyy = date.getFullYear()
  const mm = String(date.getMonth() + 1).padStart(2, '0')
  const dd = String(date.getDate()).padStart(2, '0')
  const hh = String(date.getHours()).padStart(2, '0')
  const mi = String(date.getMinutes()).padStart(2, '0')
  return `${yyyy}.${mm}.${dd} ${hh}:${mi}`
}

/** datetime-local input value: 2026-08-08T00:00 */
export function toDateTimeLocal(value) {
  const date = parseApiDate(value)
  if (!date) return ''
  const yyyy = date.getFullYear()
  const mm = String(date.getMonth() + 1).padStart(2, '0')
  const dd = String(date.getDate()).padStart(2, '0')
  const hh = String(date.getHours()).padStart(2, '0')
  const mi = String(date.getMinutes()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd}T${hh}:${mi}`
}

/** API 전송용: 2026-08-08T00:00:00 */
export function fromDateTimeLocal(value) {
  if (!value) return value
  if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(value)) {
    return `${value}:00`
  }
  return value
}

function parseApiDate(value) {
  if (value instanceof Date) {
    return Number.isNaN(value.getTime()) ? null : value
  }
  if (typeof value !== 'string') return null

  // 로컬 시각으로 파싱 (타임존 없는 ISO)
  const match = value.match(
    /^(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2})(?::(\d{2}))?/,
  )
  if (!match) {
    const fallback = new Date(value)
    return Number.isNaN(fallback.getTime()) ? null : fallback
  }
  const [, y, m, d, h, mi, s = '0'] = match
  return new Date(Number(y), Number(m) - 1, Number(d), Number(h), Number(mi), Number(s))
}
