#!/usr/bin/env bash
# Copies the web-free IPT 3 packages from the port baseline into ipt-core, verbatim.
# Re-runnable: it overwrites the copied files, so run it only on a clean tree and review the diff.
#   tools/port/port-core.sh [legacy-repo] [baseline]
set -euo pipefail

LEGACY=${1:-$HOME/code/gbif/ipt}
B=${2:-$(sed -n 's/^baseline: //p' "$(dirname "$0")/../../docs/BASELINE.md")}
ROOT=$(cd "$(dirname "$0")/../.." && pwd)
CORE="$ROOT/ipt-core"
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT

git -C "$LEGACY" archive "$B" src/main/java src/main/resources src/test | tar -x -C "$TMP"
J="$TMP/src/main/java"
T="$TMP/src/test/java"

copy_tree() { # src-dir dest-dir
  mkdir -p "$2"
  (cd "$1" && find . -type f) | while read -r f; do
    mkdir -p "$2/$(dirname "$f")"
    cp "$1/$f" "$2/$f"
  done
}

# main sources
for p in model service task validation utils config; do
  copy_tree "$J/org/gbif/ipt/$p" "$CORE/src/main/java/org/gbif/ipt/$p"
done
copy_tree "$J/org/gbif/datapackage" "$CORE/src/main/java/org/gbif/datapackage"
cp "$J/org/gbif/ipt/i18n/I18n.java" "$CORE/src/main/java/org/gbif/ipt/i18n/I18n.java"

# web-only or replaced classes
for f in config/IPTModule config/SetupAction config/LoggingConfiguration config/LoggingConfigFactory \
         config/DataDirTemplateLoader; do
  rm -f "$CORE/src/main/java/org/gbif/ipt/$f.java"
done

# portal helper classes used by the model move to org.gbif.ipt.model.metadata
mkdir -p "$CORE/src/main/java/org/gbif/ipt/model/metadata"
for c in OrganizedTaxonomicCoverage OrganizedTaxonomicKeywords; do
  sed 's/^package org\.gbif\.ipt\.action\.portal;/package org.gbif.ipt.model.metadata;/' \
    "$J/org/gbif/ipt/action/portal/$c.java" > "$CORE/src/main/java/org/gbif/ipt/model/metadata/$c.java"
done
grep -rl 'org\.gbif\.ipt\.action\.portal\.OrganizedTaxonomic' "$CORE/src/main/java" | while read -r f; do
  sed -i '' 's/org\.gbif\.ipt\.action\.portal\.OrganizedTaxonomic/org.gbif.ipt.model.metadata.OrganizedTaxonomic/g' "$f"
done

# main resources needed by core (Struts *-conversion.properties belong to the web layer)
R="$TMP/src/main/resources"
copy_tree "$R/configDefault" "$CORE/src/main/resources/configDefault"
cp "$R/jdbc.properties" "$CORE/src/main/resources/jdbc.properties"
(cd "$R" && find org -type f ! -name '*-conversion.properties') | while read -r f; do
  mkdir -p "$CORE/src/main/resources/$(dirname "$f")"
  cp "$R/$f" "$CORE/src/main/resources/$f"
done

# tests: plain unit tests of the ported packages plus the shared mocks and fixtures
for p in model service task validation utils config mock; do
  [ -d "$T/org/gbif/ipt/$p" ] && copy_tree "$T/org/gbif/ipt/$p" "$CORE/src/test/java/org/gbif/ipt/$p"
done
cp "$T/org/gbif/ipt/IptBaseTest.java" "$CORE/src/test/java/org/gbif/ipt/IptBaseTest.java"
copy_tree "$TMP/src/test/resources" "$CORE/src/test/resources"
# Struts type converter tests and servlet-context tests belong to the web layer
for f in validation/LatitudeFormatConverterTest validation/LongitudeFormatConverterTest \
         validation/CoordinateFormatConverterToStringTest config/IPTContextListenerTest config/MockServletContext; do
  rm -f "$CORE/src/test/java/org/gbif/ipt/$f.java"
done

echo "ported baseline $B into $CORE"
