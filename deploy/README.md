# 배포

EC2에서 Docker Compose로 올리는 스크립트입니다.

| 파일 | 용도 |
|------|------|
| `.env.prod.example` | 환경변수 템플릿 → `.env.prod`로 복사해 사용 |
| `ec2-setup.sh` | Docker 설치 |
| `deploy.sh` | HTTP 배포 |
| `deploy-https.sh` | Caddy HTTPS 배포 |
| `Caddyfile` | 도메인·인증서 |

```bash
cp deploy/.env.prod.example deploy/.env.prod
# 비밀번호·JWT·도메인 수정
./deploy/ec2-setup.sh
./deploy/deploy.sh
```

`.env.prod`에는 실제 시크릿을 넣고, Git에는 올리지 마세요.
