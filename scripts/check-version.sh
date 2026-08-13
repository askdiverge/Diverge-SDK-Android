#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
VERSION="$(tr -d '[:space:]' < "$ROOT/VERSION")"
fail=0
if ! grep -qE "ai\\.askdiverge:diverge-sdk:${VERSION}" "$ROOT/README.md"; then
  echo "Version mismatch in README.md"; fail=1
fi
[[ "$fail" -eq 0 ]] || { echo "Run ./scripts/sync-version.sh"; exit 1; }
echo "Version check passed ($VERSION)"
