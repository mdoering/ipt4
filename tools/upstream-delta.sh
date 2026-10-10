#!/usr/bin/env bash
# Lists IPT 3 commits made after the IPT4 port baseline, each changed file tagged with where it lands in IPT4.
# Usage: tools/upstream-delta.sh [from (default: baseline)] [to (default: origin/dwc-dp)]
set -euo pipefail
here="$(cd "$(dirname "$0")" && pwd)"
LEGACY=${LEGACY:-$HOME/code/gbif/ipt}
from=${1:-$(sed -n 's/^baseline: //p' "$here/../docs/BASELINE.md")}
to=${2:-origin/dwc-dp}

git -C "$LEGACY" fetch --quiet origin || echo "warning: could not fetch $LEGACY, using local refs" >&2
git -C "$LEGACY" log --reverse --format='%n## %h %s (%an, %as)' --name-only "$from..$to" |
  while IFS= read -r line; do
    case "$line" in
      "## "* | "") echo "$line" ;;
      src/main/java/org/gbif/ipt/action/* | src/main/java/org/gbif/ipt/struts2/*) echo "  web    $line" ;;
      src/main/resources/struts*) echo "  web    $line" ;;
      src/main/resources/ApplicationResources_*) echo "  i18n   $line" ;;
      src/main/java/* | src/main/resources/*) echo "  core   $line" ;;
      src/main/webapp/WEB-INF/pages/*) echo "  ftl    $line" ;;
      src/main/webapp/*) echo "  static $line" ;;
      src/test/*) echo "  test   $line" ;;
      *) echo "  other  $line" ;;
    esac
  done
