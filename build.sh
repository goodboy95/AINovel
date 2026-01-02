#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
SUDO_CMD=(sudo -S -p "")

run_sudo() {
  echo "123456" | "${SUDO_CMD[@]}" "$@"
}

# Frontend build & tests
cd "$ROOT_DIR/frontend"
npm ci --legacy-peer-deps
if npm run | grep -q "^  test"; then
  npm run test
fi
npm run build
cd "$ROOT_DIR"

# Backend build & tests
if [ -d "$ROOT_DIR/backend/target" ]; then
  rm -rf "$ROOT_DIR/backend/target"
fi
cd "$ROOT_DIR/backend"
mvn -q test
mvn -q -DskipTests clean package
shopt -s nullglob
JAR_PATH=""
for jar in "$ROOT_DIR/backend/target/"*.jar; do
  if [[ "$jar" != *.original ]]; then
    JAR_PATH="$jar"
    break
  fi
done
shopt -u nullglob
if [ -z "$JAR_PATH" ]; then
  echo "Backend jar not found in $ROOT_DIR/backend/target"
  exit 1
fi
cp "$JAR_PATH" "$ROOT_DIR/backend/target/app.jar"
cd "$ROOT_DIR"

# Docker compose rollout
run_sudo docker compose down
run_sudo docker compose up -d

echo "Deployment finished. Frontend: http://ainovel.seekerhut.com:10001. Backend API: http://ainovel.seekerhut.com:20001/api"
