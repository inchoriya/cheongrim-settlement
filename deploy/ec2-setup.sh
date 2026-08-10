#!/usr/bin/env bash
# Amazon Linux 2023 / Ubuntu 22.04+ 에서 Docker Compose 설치
set -euo pipefail

echo "[1/4] Detect OS..."
if [ -f /etc/os-release ]; then
  # shellcheck disable=SC1091
  . /etc/os-release
else
  echo "Unsupported OS"
  exit 1
fi

echo "[2/4] Install Docker..."
if [[ "${ID}" == "amzn" ]]; then
  sudo dnf update -y
  sudo dnf install -y docker git
  sudo systemctl enable --now docker
  sudo usermod -aG docker "$USER" || true
elif [[ "${ID}" == "ubuntu" ]]; then
  sudo apt-get update -y
  sudo apt-get install -y ca-certificates curl git
  sudo install -m 0755 -d /etc/apt/keyrings
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
  echo \
    "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
    $(. /etc/os-release && echo \"$VERSION_CODENAME\") stable" | \
    sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
  sudo apt-get update -y
  sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
  sudo usermod -aG docker "$USER" || true
else
  echo "Please install Docker manually for ID=${ID}"
  exit 1
fi

echo "[3/4] Verify..."
sudo docker version
sudo docker compose version || docker compose version

echo "[4/4] Done."
echo "새 그룹 적용을 위해 로그아웃 후 다시 로그인하세요."
echo "다음: 프로젝트 클론 → deploy/.env.prod 작성 → ./deploy/deploy.sh"
