#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
COMPOSE_FILE="$ROOT_DIR/deploy/docker-compose.yml"
PROJECT_NAME="${PROJECT_NAME:-ainovel-deps}"
SUDO_PASS="${SUDO_PASS:-123456}"

run_sudo() {
  if [[ "$EUID" -eq 0 ]]; then
    "$@"
  else
    echo "$SUDO_PASS" | sudo -S -p "" "$@"
  fi
}

if [[ ! -f "$COMPOSE_FILE" ]]; then
  echo "Missing compose file: $COMPOSE_FILE"
  exit 1
fi

run_sudo mkdir -p "$ROOT_DIR/deploy/mysql/data" "$ROOT_DIR/deploy/redis/data"

run_sudo docker compose -p "$PROJECT_NAME" -f "$COMPOSE_FILE" down --remove-orphans
run_sudo docker compose -p "$PROJECT_NAME" -f "$COMPOSE_FILE" up -d

echo "Dependency containers started."
