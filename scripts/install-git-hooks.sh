#!/usr/bin/env bash
# Enable local Git hooks for this clone.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
git -C "$ROOT" config core.hooksPath .githooks
echo "hooksPath -> .githooks"
