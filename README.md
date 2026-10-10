# IPT 4

The GBIF Integrated Publishing Toolkit, rebuilt on Spring Boot 4 and Java 25.
The design is in [docs/design.md](docs/design.md). It is ported from IPT 3 at the commit named in [docs/BASELINE.md](docs/BASELINE.md).

## Build and run

```bash
mvn -B verify
mvn -B -DskipTests install && IPT_DATA_DIR=~/ipt4-data mvn -pl ipt-app spring-boot:run -Dspring-boot.run.profiles=dev   # http://localhost:7001
docker build -f docker/Dockerfile -t gbif/ipt:4.0.0-SNAPSHOT . && docker run -p 8080:8080 -v ipt4data:/srv/ipt gbif/ipt:4.0.0-SNAPSHOT
```
