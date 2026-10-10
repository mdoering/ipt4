#!/usr/bin/env bash
# Builds the IPT4 image, starts it on a fresh data dir and checks health, the setup wizard, file logging and the
# non-root user.
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
# a fresh data dir sends every page to the setup wizard
test "$(curl -s -o /dev/null -w '%{redirect_url}' "http://localhost:$port/about.do")" = \
  "http://localhost:$port/setupDefaultAdministrator.do"
curl -fs "http://localhost:$port/setupDefaultAdministrator.do" | grep 'IPT Default administrator' > /dev/null
curl -fs "http://localhost:$port/setupDefaultAdministrator.do?request_locale=fr" \
  | grep "Administrateur par défaut de l&#39;IPT\|Administrateur par défaut de l'IPT" > /dev/null
docker exec "$name" test -s /srv/ipt/logs/debug.log
test "$(docker exec "$name" id -un)" = "ipt"
echo "docker smoke test: OK"
