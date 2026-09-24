#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

echo "=========================================="
echo "  MY CHHACHH - LOCAL APK BUILD"
echo "=========================================="

chmod +x gradlew
./gradlew --no-daemon clean assembleDebug

SRC="app/build/outputs/apk/debug/app-debug.apk"
OUT="MY_CHHACHH_LATEST_DEBUG.apk"

test -f "$SRC"
cp -f "$SRC" "$OUT"
echo
echo "SUCCESS"
echo "APK: $(pwd)/$OUT"
