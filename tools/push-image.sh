#!/usr/bin/env bash
# Builds and pushes the release image to a repository as <repo>:<version>, a final version (no suffix) also as
# <repo>:latest, for every platform of tools/docker-publish.sh. Expects a docker login to that registry.
set -euo pipefail
repo=${1:?usage: tools/push-image.sh <repo> <version>}
version=${2:?usage: tools/push-image.sh <repo> <version>}
tags=("$repo:$version")
if [[ "$version" != *-* ]]; then tags+=("$repo:latest"); fi
"$(dirname "$0")/docker-publish.sh" "${tags[@]}"
