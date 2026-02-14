#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="$SCRIPT_DIR/docker-compose.yml"
ENV_FILE="$SCRIPT_DIR/.env"
ENV_EXAMPLE="$SCRIPT_DIR/.env.example"

get_env_value() {
  local key="$1"
  local file="$2"
  local value
  value="$(awk -F= -v k="$key" '
    $0 ~ "^[[:space:]]*"k"=" {
      sub(/^[^=]*=/, "", $0)
      print $0
    }
  ' "$file" | tail -n 1)"
  value="${value%$'\r'}"
  value="${value#\"}"
  value="${value%\"}"
  value="${value#\'}"
  value="${value%\'}"
  printf '%s' "$value"
}

log() {
  printf '[update] %s\n' "$*"
}

fail() {
  printf '[update] ERROR: %s\n' "$*" >&2
  exit 1
}

if ! command -v docker >/dev/null 2>&1; then
  fail "Docker is not installed. Install Docker Desktop first."
fi

if ! docker info >/dev/null 2>&1; then
  fail "Docker daemon is not running. Start Docker Desktop and retry."
fi

if docker compose version >/dev/null 2>&1; then
  COMPOSE_CMD=(docker compose)
elif command -v docker-compose >/dev/null 2>&1; then
  COMPOSE_CMD=(docker-compose)
else
  fail "Docker Compose not found. Install Docker Desktop (Compose v2)."
fi

if [[ ! -f "$ENV_FILE" ]]; then
  if [[ -f "$ENV_EXAMPLE" ]]; then
    cp "$ENV_EXAMPLE" "$ENV_FILE"
    log "Created $ENV_FILE from .env.example."
  else
    fail "Missing $ENV_FILE and $ENV_EXAMPLE."
  fi
fi

BACKEND_IMAGE="$(get_env_value "BACKEND_IMAGE" "$ENV_FILE")"
WEB_IMAGE="$(get_env_value "WEB_IMAGE" "$ENV_FILE")"
WATCHTOWER_ENABLED="$(get_env_value "WATCHTOWER_ENABLED" "$ENV_FILE")"
WATCHTOWER_ENABLED="${WATCHTOWER_ENABLED:-true}"

if [[ -z "$BACKEND_IMAGE" || "$BACKEND_IMAGE" == *"CHANGE_ME"* ]]; then
  fail "Set BACKEND_IMAGE in $ENV_FILE (example: ghcr.io/my-org/print-business-backend:latest)."
fi

if [[ -z "$WEB_IMAGE" || "$WEB_IMAGE" == *"CHANGE_ME"* ]]; then
  fail "Set WEB_IMAGE in $ENV_FILE (example: ghcr.io/my-org/print-business-web:latest)."
fi

log "Pulling latest images..."
"${COMPOSE_CMD[@]}" --env-file "$ENV_FILE" -f "$COMPOSE_FILE" pull

PROFILE_ARGS=()
case "${WATCHTOWER_ENABLED,,}" in
  true|1|yes|on)
    PROFILE_ARGS=(--profile watchtower)
    ;;
esac

log "Recreating services with fresh images..."
"${COMPOSE_CMD[@]}" --env-file "$ENV_FILE" -f "$COMPOSE_FILE" "${PROFILE_ARGS[@]}" up -d --remove-orphans

log "Pruning old dangling images..."
docker image prune -f >/dev/null 2>&1 || true

log "Service status:"
"${COMPOSE_CMD[@]}" --env-file "$ENV_FILE" -f "$COMPOSE_FILE" ps

log "Update complete."
