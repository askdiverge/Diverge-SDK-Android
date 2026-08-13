#!/usr/bin/env bash
# Sync VERSION into README (Android).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
VERSION="$(tr -d '[:space:]' < "$ROOT/VERSION")"

if [[ ! "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+([.-].+)?$ ]]; then
  echo "Invalid VERSION: '$VERSION'" >&2
  exit 1
fi

replace_in_file() {
  local file="$1"
  local expression="$2"
  if [[ ! -f "$file" ]]; then
    echo "Skip missing file: $file" >&2
    return 0
  fi
  sed -i.bak -E "$expression" "$file"
  rm -f "${file}.bak"
}

replace_in_file "$ROOT/README.md" \
  "s|(ai\\.askdiverge:diverge-sdk:)[0-9]+\\.[0-9]+\\.[0-9]+([.-][^\"[:space:]]*)?|\\1${VERSION}|"

echo "Synced version $VERSION"
