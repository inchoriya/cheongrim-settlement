#!/usr/bin/env bash
# EC2에서 정산관리 프로덕션 Compose 배포/재배포
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ROOT_DIR}/deploy/.env.prod"
COMPOSE_FILE="${ROOT_DIR}/docker-compose.prod.yml"

cd "${ROOT_DIR}"

if [ ! -f "${ENV_FILE}" ]; then
  echo "Missing ${ENV_FILE}"
  echo "Copy template: cp deploy/.env.prod.example deploy/.env.prod"
  exit 1
fi

echo "[deploy] Using ${COMPOSE_FILE}"
docker compose -f "${COMPOSE_FILE}" --env-file "${ENV_FILE}" pull || true
docker compose -f "${COMPOSE_FILE}" --env-file "${ENV_FILE}" up -d --build --remove-orphans

echo "[deploy] Status"
docker compose -f "${COMPOSE_FILE}" --env-file "${ENV_FILE}" ps

echo "[deploy] Health (api via web proxy)"
sleep 3
curl -fsS "http://127.0.0.1/actuator/health" || curl -fsS "http://127.0.0.1:8080/actuator/health" || true

echo
echo "UI: http://<EC2_PUBLIC_IP>"
echo "Demo: admin@cheongrim.local / Demo1234!  (SEED_ENABLED=true 인 경우)"
