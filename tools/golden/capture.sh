#!/usr/bin/env bash
# Captures responses of a running IPT 3 as golden files for IPT4 parity tests (read-only GET/HEAD requests).
# Usage: tools/golden/capture.sh <ipt3-base-url> <out-dir> [resource-shortname ...]
set -euo pipefail
base=${1:?IPT 3 base URL, e.g. https://ipt.gbif.org}
out=${2:?output directory}
shift 2
mkdir -p "$out"

capture() { # <name> <path>
  curl -sS -D "$out/$1.headers" -o "$out/$1.body" -w '%{http_code}\n' "$base$2" > "$out/$1.status"
}

capture inventory-dataset /inventory/dataset
capture inventory-v2-dataset /inventory/v2/dataset
capture api-resources /api/resources
capture api-health /api/health
capture rss /rss.do
capture dcat /dcat.do
capture about /about.do

for r in "$@"; do
  capture "resource-$r" "/resource?r=$r"
  capture "eml-$r" "/eml.do?r=$r"
  capture "rtf-$r" "/rtf.do?r=$r"
  curl -sS -I "$base/archive.do?r=$r" > "$out/archive-$r.headers"
  modified=$(grep -i '^last-modified:' "$out/archive-$r.headers" | cut -d' ' -f2- | tr -d '\r' || true)
  if [ -n "$modified" ]; then
    curl -sS -o /dev/null -w '%{http_code}\n' -H "If-Modified-Since: $modified" "$base/archive.do?r=$r" \
      > "$out/archive-$r.if-modified-since.status"
  fi
done
echo "Captured $(ls "$out" | wc -l | tr -d ' ') files into $out"
