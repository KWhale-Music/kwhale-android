#!/usr/bin/env bash
set -euo pipefail

# Reproducible JADX dump of the VK X APK.
#
# Usage:
#   tools/decompile.sh [output-dir]
#
# Requirements:
#   - JDK 17+ (JDK 21 works)
#   - jadx on PATH, or set JADX=/path/to/jadx
#
# The APK is decoded into re/decoded by apktool; classes*.dex live there.

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT="${1:-$ROOT/re/decompiled/sources}"
JADX="${JADX:-$(command -v jadx || true)}"

if [[ -z "$JADX" ]]; then
  echo "jadx not found; set JADX=/path/to/jadx" >&2
  exit 1
fi

if [[ -n "${JAVA_HOME:-}" ]]; then
  export PATH="$JAVA_HOME/bin:$PATH"
fi

DEX_DIR="$ROOT/re/decoded"
for dex in classes.dex classes2.dex classes3.dex; do
  if [[ ! -f "$DEX_DIR/$dex" ]]; then
    echo "missing $DEX_DIR/$dex" >&2
    exit 1
  fi
done

rm -rf "$OUT"
mkdir -p "$OUT"

"$JADX" \
  -d "$OUT" \
  --no-res \
  -j "${JADX_JOBS:-8}" \
  "$DEX_DIR/classes.dex" \
  "$DEX_DIR/classes2.dex" \
  "$DEX_DIR/classes3.dex"

echo "done: $(find "$OUT" -type f -name '*.java' | wc -l) java files -> $OUT"
