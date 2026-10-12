#!/usr/bin/env bash
# Pushes the release image gbif/ipt:<version> to a repository as <repo>:<version>, a final version (no suffix) also
# as <repo>:latest. Expects a docker login to that registry.
set -euo pipefail
repo=${1:?usage: tools/push-image.sh <repo> <version>}
version=${2:?usage: tools/push-image.sh <repo> <version>}
tags=("$version")
if [[ "$version" != *-* ]]; then tags+=(latest); fi
for tag in "${tags[@]}"; do
  docker tag "gbif/ipt:$version" "$repo:$tag"
  docker push "$repo:$tag"
done
