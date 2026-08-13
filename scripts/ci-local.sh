#!/usr/bin/env bash
# Local CI for Android (mirrors .github/workflows/android.yml).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if [[ "${DIVERGE_SKIP_LOCAL_CI:-}" == "1" ]]; then
  echo "DIVERGE_SKIP_LOCAL_CI=1 set — skipping local CI"
  exit 0
fi

echo "==> CI local: version sync"
./scripts/check-version.sh

echo "==> CI local: Android"
./gradlew \
  :diverge-sdk:assemble \
  :diverge-sdk:test \
  :diverge-sdk:lint \
  :diverge-sdk:dokkaHtml \
  :diverge-sdk:dokkaJavadoc \
  :sample:assembleDebug \
  :sample:assembleRelease \
  :sample:verifyR8PublicApiKeeps \
  :sample:testDebugUnitTest \
  --stacktrace

echo "==> CI local: OK"
