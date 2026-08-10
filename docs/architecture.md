# 구조 개요

```text
React 관리 화면  ──►  Spring Boot API (JWT)
                         ├── MySQL / H2
                         ├── Redis (Compose)
                         └── PayoutGateway (Mock | Toss)
```

로컬은 Docker Compose로 `web` / `api` / `mysql` / `redis`를 함께 띄울 수 있습니다.  
배포용으로는 `docker-compose.prod.yml`과 `deploy/` 스크립트를 두었습니다.

## 백엔드 패키지

```text
com.settlehub
  auth / organization / policy / order
  settlement   … 계산, 배치, 상태
  payout       … PayoutGateway, Mock, Toss
  dashboard / ops(audit) / config
```

| 컴포넌트 | 역할 |
|----------|------|
| SettlementCalculator | 순수 정산 계산 |
| PolicyResolver | 대행사 정책 → 전역 정책 |
| SettlementBatchService | 기간 집계, 중복 방지 |
| PayoutGateway | 지급 추상화 |

## 인증·인가

- JWT + BCrypt
- 역할: ADMIN / AGENCY / MERCHANT
- 목록·상세 조회 시 소속 대행사·가맹점 범위를 서비스에서 강제

## 정산 배치

1. 기간 안 `CREATED`·미잠금 주문 조회  
2. 정책 해석 후 주문별 계산  
3. 가맹점 단위로 헤더·라인 저장, 주문 잠금  
4. 관리자가 확정 → 지급대기 → 지급  

## 지급

`PAYOUT_PROVIDER=mock|toss` 로 구현체를 바꿉니다.  
토스 모드는 지급대행 API와 JWE 암호화를 사용하며, 키는 환경변수로만 주입합니다.

## 주요 API 영역

`/api/v1/auth`, `orders`, `settlements`, `payouts`, `policies`, `organizations`, `dashboard`, `audit-logs`
