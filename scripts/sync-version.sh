#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
VERSION="$(tr -d '[:space:]' < "$ROOT/VERSION")"
[[ "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+([.-].+)?$ ]] || { echo "Invalid VERSION"; exit 1; }
if [[ -f "$ROOT/README.md" ]]; then
  sed -i.bak -E "s|(ai\\.askdiverge:diverge-sdk:)[0-9]+\\.[0-9]+\\.[0-9]+([.-][^\"[:space:]]*)?|\\1${VERSION}|" "$ROOT/README.md"
  rm -f "$ROOT/README.md.bak"
fi
echo "Synced version $VERSION"
