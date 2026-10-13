# IPT 4

The GBIF Integrated Publishing Toolkit, rebuilt on Spring Boot 4 and Java 25.
The design is in [docs/design.md](docs/design.md). User documentation is the Antora component in [docs/modules/ROOT/pages](docs/modules/ROOT/pages); what is covered and what is left before 4.0 is in [docs/parity-checklist.md](docs/parity-checklist.md). It is ported from IPT 3 at the commit named in [docs/BASELINE.md](docs/BASELINE.md).

## Run with Docker

IPT4 is experimental and not approved by GBIF yet. Its images are published to this repository's GitHub Container
Registry, not to Docker Hub (`gbif/ipt`), and can be pulled without a login:

| Image | What it is |
|---|---|
| `ghcr.io/mdoering/ipt4:main` | the latest green build of `main` |
| `ghcr.io/mdoering/ipt4:sha-<commit>` | the build of one commit of `main`, e.g. `sha-dae724c` |
| `ghcr.io/mdoering/ipt4:<version>` | a release, e.g. `4.0.0-RC1`, from a pushed tag `v<version>` |
| `ghcr.io/mdoering/ipt4:latest` | the latest final release (no `-RC` or other suffix) |

The images are built for `linux/amd64`; on Apple Silicon Docker runs them emulated, slower but working
(`--platform linux/amd64` silences the warning).

```bash
docker run -d --name ipt -p 8080:8080 -v ipt-data:/srv/ipt ghcr.io/mdoering/ipt4:main
```

Open http://localhost:8080/ and the setup asks for the first administrator, the registry mode (test or production)
and the public URL. The data lives in the volume `ipt-data`, mounted at `/srv/ipt`; keep the volume, and with it
`config/.secret.key`, which encrypts the stored passwords. The container runs as the non-root user `ipt` and reports
its health at `/actuator/health`. Logs go to `docker logs ipt` and to `logs/` in the data directory.

A host directory instead of a volume must be writable by the container user; run as yourself with `--user`:

```bash
mkdir -p ~/ipt4-data
docker run -d --name ipt -p 8080:8080 --user "$(id -u):$(id -g)" -v ~/ipt4-data:/srv/ipt ghcr.io/mdoering/ipt4:main
```

Deployment settings are environment variables, e.g. `-e SERVER_SERVLET_SESSION_COOKIE_SECURE=true` when the IPT is
served over HTTPS only; all of them are in
[docs/modules/ROOT/pages/configuration.adoc](docs/modules/ROOT/pages/configuration.adoc).

To upgrade, pull the new image and start it on the same data, after a backup of the volume:

```bash
docker pull ghcr.io/mdoering/ipt4:main
docker rm -f ipt
docker run -d --name ipt -p 8080:8080 -v ipt-data:/srv/ipt ghcr.io/mdoering/ipt4:main
```

An IPT 3 data directory is converted once into a new volume by the `migrate` command, which only reads the old
directory (it must be readable by the container user). Check with `--dry-run` first, then start IPT4 on the new volume
as above:

```bash
docker run --rm -v /var/lib/ipt3:/old:ro -v ipt-data:/srv/ipt ghcr.io/mdoering/ipt4:main migrate --from /old --to /srv/ipt --dry-run
docker run --rm -v /var/lib/ipt3:/old:ro -v ipt-data:/srv/ipt ghcr.io/mdoering/ipt4:main migrate --from /old --to /srv/ipt
```

More in [migration.adoc](docs/modules/ROOT/pages/migration.adoc).

## Build and run

```bash
mvn -B verify
mvn -B -DskipTests install && IPT_DATA_DIR=~/ipt4-data mvn -pl ipt-app spring-boot:run -Dspring-boot.run.profiles=dev   # http://localhost:7001
docker build -f docker/Dockerfile -t gbif/ipt:4.0.0-SNAPSHOT . && docker run -p 8080:8080 -v ipt4data:/srv/ipt gbif/ipt:4.0.0-SNAPSHOT
```

## Data directory

IPT4 needs a data directory, set with `IPT_DATA_DIR` (or `ipt.data-dir`). An empty or missing directory is set up on first start.
IPT 3 data directories are not used directly: convert a copy once with the `migrate` command, which only reads the old directory.

```bash
java -jar ipt-app/target/ipt-4.0.0-SNAPSHOT.jar migrate --from /var/lib/ipt3 --to ~/ipt4-data [--dry-run] [--link-artifacts] [--include-logs]
docker run --rm -v /var/lib/ipt3:/old:ro -v ipt4data:/srv/ipt gbif/ipt:4.0.0-SNAPSHOT migrate --from /old --to /srv/ipt
```

The migration writes `migration-report.txt` into the new directory. Stored passwords are re-encrypted with a new key in `config/.secret.key`; keep that file with the data (or set `IPT_SECRET_KEY`).
