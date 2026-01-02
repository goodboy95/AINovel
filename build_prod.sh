#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"

"$ROOT_DIR/build.sh"

echo "Production deployment finished. URL: http://ainovel.seekerhut.com:10001"
