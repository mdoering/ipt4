#!/usr/bin/env bash
# Builds the IPT4 image for linux/amd64 and linux/arm64 (PLATFORMS overrides) and pushes it under every <image:tag>
# given. Expects a docker login to the registry and a buildx builder for those platforms (QEMU for a foreign one).
set -euo pipefail
if [ $# -eq 0 ]; then
  echo "usage: tools/docker-publish.sh <image:tag>..." >&2
  exit 1
fi
args=()
for tag in "$@"; do args+=(-t "$tag"); done
docker buildx build --platform "${PLATFORMS:-linux/amd64,linux/arm64}" -f docker/Dockerfile "${args[@]}" --push .
