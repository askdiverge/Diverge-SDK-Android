#!/usr/bin/env bash
# Verify VERSION matches synced files (Android).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
VERSION="$(tr -d '[:space:]' < "$ROOT/VERSION")"

fail=0

check_contains() {
  local file="$1"
  local pattern="$2"
  if [[ ! -f "$file" ]]; then
    echo "Missing file: $file" >&2
    fail=1
    return
  fi
  if ! grep -qE "$pattern" "$file"; then
    echo "Version mismatch in $file (expected $VERSION)" >&2
    fail=1
  fi
}

if [[ ! "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+([.-].+)?$ ]]; then
  echo "Invalid VERSION: $VERSION" >&2
  fail=1
fi

check_contains "$ROOT/README.md" "ai\\.askdiverge:diverge-sdk:${VERSION}"

if [[ "$fail" -ne 0 ]]; then
  echo "Run: ./scripts/sync-version.sh" >&2
  exit 1
fi

echo "Version check passed ($VERSION)"
