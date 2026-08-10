# Backend

Spring Boot 정산 API입니다.

## 실행

JDK 17이 필요합니다. `JAVA_HOME`은 JDK 루트 경로여야 합니다.

```powershell
.\gradlew.bat test
.\gradlew.bat bootRun
```

기본 프로필은 H2 인메모리 DB를 사용합니다.

## 데모 계정

시드가 켜져 있으면(`settlehub.seed.enabled=true`) 아래 계정이 만들어집니다.  
비밀번호: `Demo1234!`

- admin@cheongnim.local  
- agency@seoul.local  
- merchant@kimbap.local  

## 지급 설정

| 환경변수 | 설명 |
|----------|------|
| PAYOUT_PROVIDER | `mock`(기본) / `toss` |
| TOSS_SECRET_KEY | 토스 시크릿 키 |
| TOSS_SECURITY_KEY | 지급대행 JWE 보안 키 |

자세한 정산 규칙은 [docs/settlement-rules.md](../docs/settlement-rules.md)를 참고하세요.
