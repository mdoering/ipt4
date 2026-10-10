#!/usr/bin/env bash
# Builds the IPT4 image, starts it and checks health, the about page, file logging and the non-root user.
set -euo pipefail
image=${1:-gbif/ipt:4.0.0-SNAPSHOT}
port=${PORT:-18080}
name="ipt4-smoke-$$"

docker build -f docker/Dockerfile -t "$image" .
docker run -d --name "$name" -p "$port:8080" "$image" > /dev/null
trap 'docker rm -f "$name" > /dev/null' EXIT

for _ in $(seq 1 90); do
  if curl -fs "http://localhost:$port/actuator/health" | grep '"UP"' > /dev/null; then break; fi
  sleep 2
done
curl -fs "http://localhost:$port/actuator/health" | grep '"UP"' > /dev/null
curl -fs "http://localhost:$port/about.do" | grep 'About this IPT installation' > /dev/null
curl -fs "http://localhost:$port/about?request_locale=fr" | grep 'propos de cette installation IPT' > /dev/null
docker exec "$name" test -s /srv/ipt/logs/debug.log
test "$(docker exec "$name" id -un)" = "ipt"
echo "docker smoke test: OK"
