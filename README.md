# IPT 4

The GBIF Integrated Publishing Toolkit, rebuilt on Spring Boot 4 and Java 25.
The design is in [docs/design.md](docs/design.md). User documentation is the Antora component in [docs/modules/ROOT/pages](docs/modules/ROOT/pages); what is covered and what is left before 4.0 is in [docs/parity-checklist.md](docs/parity-checklist.md). It is ported from IPT 3 at the commit named in [docs/BASELINE.md](docs/BASELINE.md).

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
