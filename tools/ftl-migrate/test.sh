#!/usr/bin/env bash
# Tests FtlMigrate: expected output, idempotency, and warnings.
set -euo pipefail
here="$(cd "$(dirname "$0")" && pwd)"
out="$(mktemp -d)"
trap 'rm -rf "$out"' EXIT

java "$here/FtlMigrate.java" "$here/test/input" "$out/once" 2> "$out/report.txt"
diff -ru "$here/test/expected" "$out/once"

java "$here/FtlMigrate.java" "$out/once" "$out/twice" 2> /dev/null
diff -ru "$here/test/expected" "$out/twice"

grep -q "angle.ftl:9: Struts/OGNL construct '%{' needs manual migration" "$out/report.txt"
grep -q "script.ftl:3: POST form not at line start, add the CSRF field manually" "$out/report.txt"
echo "ftl-migrate: OK"
