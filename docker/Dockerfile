# syntax=docker/dockerfile:1
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /src
COPY . .
RUN --mount=type=cache,target=/root/.m2 mvn -B -DskipTests package

FROM eclipse-temurin:25-jre
LABEL org.opencontainers.image.title="GBIF IPT 4" \
      org.opencontainers.image.source="https://github.com/gbif/ipt"
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system ipt \
    && useradd --system --gid ipt --home-dir /opt/ipt ipt \
    && mkdir -p /opt/ipt /srv/ipt \
    && chown ipt:ipt /srv/ipt
ENV IPT_DATA_DIR=/srv/ipt
VOLUME /srv/ipt
COPY --from=build /src/ipt-app/target/ipt-*.jar /opt/ipt/ipt.jar
USER ipt
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
  CMD curl -fs http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/opt/ipt/ipt.jar"]
