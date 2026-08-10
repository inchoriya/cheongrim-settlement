#!/usr/bin/env bash
# EC2 HTTPS 배포 (prod + Caddy)
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ROOT_DIR}/deploy/.env.prod"
COMPOSE_FILES=(-f "${ROOT_DIR}/docker-compose.prod.yml" -f "${ROOT_DIR}/docker-compose.https.yml")

cd "${ROOT_DIR}"

if [ ! -f "${ENV_FILE}" ]; then
  echo "Missing ${ENV_FILE}"
  echo "Copy: cp deploy/.env.prod.example deploy/.env.prod"
  exit 1
fi

# shellcheck disable=SC1090
set -a
source "${ENV_FILE}"
set +a

if [ -z "${DOMAIN:-}" ] || [ -z "${ACME_EMAIL:-}" ]; then
  echo "DOMAIN and ACME_EMAIL must be set in deploy/.env.prod"
  exit 1
fi

echo "[https] DOMAIN=${DOMAIN}"
docker compose "${COMPOSE_FILES[@]}" --env-file "${ENV_FILE}" up -d --build --remove-orphans
docker compose "${COMPOSE_FILES[@]}" --env-file "${ENV_FILE}" ps

echo "[https] Waiting for certificate / proxy..."
sleep 5
curl -fsSI "https://${DOMAIN}/actuator/health" || curl -fsSI "http://${DOMAIN}/actuator/health" || true

echo
echo "UI:  https://${DOMAIN}"
echo "API: https://${DOMAIN}/api/v1"
echo "If cert fails: check DNS A record, SG 80/443, and caddy logs:"
echo "  docker logs settlehub-caddy"
