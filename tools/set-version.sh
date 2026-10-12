#!/usr/bin/env bash
# Sets the Maven version of all modules for a release; refuses anything but <major>.<minor>.<patch>[-<suffix>].
set -euo pipefail
version=${1:?usage: tools/set-version.sh <version>}
if ! [[ "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+(-[A-Za-z0-9.]+)?$ ]]; then
  echo "version '$version' is not <major>.<minor>.<patch>[-<suffix>]" >&2
  exit 1
fi
mvn -B versions:set -DnewVersion="$version" -DgenerateBackupPoms=false
