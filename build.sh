#!/usr/bin/env bash
set -e

# Frontend build
cd frontend
npm ci --legacy-peer-deps
npm run build
cd ..

# Backend build
test -d backend/target && rm -rf backend/target
cd backend
mvn -q -DskipTests clean package
cd ..

# Docker compose rollout
docker compose down
docker compose build
docker compose up -d

echo "Deployment finished. Frontend: http://localhost (default). Backend API: http://localhost:8080/api"
