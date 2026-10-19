#!/usr/bin/env bash
# Dev helper: launch the sample with a session token Intent extra (avoids adb input
# text, which mangles '_' in JWTs). Token file is never committed.
# Usage: scripts/paste-sample-token.sh [/path/to/token.txt]
set -euo pipefail

TOKEN_FILE="${1:-/tmp/boozt-se-token.txt}"
TOKEN=$(tr -d '\n\r' < "$TOKEN_FILE")
echo "token_len=${#TOKEN}"

export PATH="/usr/bin:/bin:/usr/sbin:/sbin:/opt/homebrew/bin:/Users/mohamedaldahoul/Library/Android/sdk/platform-tools:${PATH}"

adb shell am force-stop ai.askdiverge.sample
adb shell am start \
  -n ai.askdiverge.sample/.MainActivity \
  --es session_token "$TOKEN"
echo DONE
