const CLASS_MAP = {
  CREATED: 'badge badge-neutral',
  CANCELLED: 'badge badge-muted',
  CALCULATED: 'badge badge-neutral',
  HELD: 'badge badge-warn',
  CONFIRMED: 'badge badge-info',
  READY_FOR_PAYOUT: 'badge badge-info',
  PAID: 'badge badge-success',
  PAYOUT_FAILED: 'badge badge-danger',
  SUCCEEDED: 'badge badge-success',
  FAILED: 'badge badge-danger',
  REQUESTED: 'badge badge-neutral',
}

const LABEL_MAP = {
  CREATED: '생성',
  CANCELLED: '취소',
  CALCULATED: '산출',
  HELD: '보류',
  CONFIRMED: '확정',
  READY_FOR_PAYOUT: '지급대기',
  PAID: '지급완료',
  PAYOUT_FAILED: '지급실패',
  SUCCEEDED: '성공',
  FAILED: '실패',
  REQUESTED: '요청',
}

export default function StatusBadge({ status }) {
  return (
    <span className={CLASS_MAP[status] || 'badge badge-neutral'}>
      {LABEL_MAP[status] || status}
    </span>
  )
}
