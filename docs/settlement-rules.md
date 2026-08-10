# 정산 규칙

코드와 테스트의 기준이 되는 계산·상태 규칙입니다.  
구현: `SettlementCalculator`, `SettlementBatchService`

## 원칙

- 통화는 원(`Long`). 비율은 bps(5% = 500)로 두고 `floor`로 계산합니다.
- 배달팁(`deliveryTip`)은 주문금액과 분리하고, 수수료 과금 베이스에서는 뺍니다.
- `CANCELLED` 주문은 정산에 넣지 않습니다.
- 한 주문은 유효 정산 라인에 한 번만 들어갑니다.
- `CONFIRMED` 이후에는 같은 기간을 다시 계산하지 않습니다.

```text
fee = floor(orderAmount × bps / 10000)

platformFee = floor(orderAmount × platformFeeBps / 10000)
agencyFee   = floor(orderAmount × agencyFeeBps / 10000)
riderFee    = 고정 라이더비

merchantSettlement = orderAmount - platformFee - agencyFee - riderFee
```

검산: `merchant + platform + agency + rider == orderAmount`

## 정책 선택

주문 시각 `orderedAt` 기준

1. 대행사 전용 정책이 있으면 사용  
2. 없으면 전역 기본 정책  
3. 둘 다 없으면 이상 건으로 잡고 보류 후보  

## 정산 상태

```text
CALCULATED → HELD / CONFIRMED
CONFIRMED  → READY_FOR_PAYOUT
READY_FOR_PAYOUT → PAID / PAYOUT_FAILED
```

- 가맹점 정산액이 음수이거나 정책이 없으면 `HELD`
- 지급 실패 시 `PAYOUT_FAILED`에서 다시 시도 가능

## 수치 예시 (기본 정책 5% / 10% / 라이더 3,000)

| 예시 | 주문금액 | 플랫폼 | 대행 | 라이더 | 가맹점 |
|------|----------|--------|------|--------|--------|
| A | 20,000 | 1,000 | 2,000 | 3,000 | 14,000 |
| B | 10,001 | 500 | 1,000 | 3,000 | 5,501 |
| G (A+B) | 30,001 | 1,500 | 3,000 | 6,000 | **19,501** |

시드 `ORD-SEED-001`, `ORD-SEED-002`가 A·B에 해당합니다.
