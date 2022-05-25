#   Copyright (c) 2021, 2022, Oracle and/or its affiliates.
#   Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

# First layer: dependencies for the project, cached in the /root/.m2 directory
FROM maven:3.6-jdk-11 as m2repo
ARG MAVEN_OPTS

WORKDIR /project/
COPY pom.xml .
COPY wls-exporter-core/pom.xml wls-exporter-core/
COPY wls-exporter-sidecar/pom.xml wls-exporter-sidecar/

RUN mvn -B -e -C org.apache.maven.plugins:maven-dependency-plugin:3.1.2:go-offline -Ddocker-build

# Now build the project on top of that first layer
FROM maven:3.6-jdk-11 as build
ARG MAVEN_OPTS

WORKDIR /project/
COPY --from=m2repo /root/.m2 /root/.m2
COPY pom.xml .
COPY wls-exporter-core/ wls-exporter-core/
COPY wls-exporter-sidecar/ wls-exporter-sidecar/

RUN mvn -B -e -C install -Ddocker-build -DskipTests=true

FROM ghcr.io/oracle/oraclelinux:8-slim AS jre-build

ENV JAVA_URL="https://download.java.net/java/GA/jdk18.0.1.1/65ae32619e2f40f3a9af3af1851d6e19/2/GPL/openjdk-18.0.1.1_linux-x64_bin.tar.gz"

RUN set -eux; \
    microdnf -y install gzip tar; \
    curl -fL -o /jdk.tar.gz "$JAVA_URL"; \
    mkdir -p /jdk; \
    tar --extract --file /jdk.tar.gz --directory /jdk --strip-components 1; \
    /jdk/bin/jlink --verbose --compress 2 --strip-java-debug-attributes --no-header-files --no-man-pages --output jre --add-modules java.base,java.desktop,java.instrument,java.management,java.naming,java.net.http,java.security.jgss,java.sql,jdk.attach,jdk.jdi,jdk.unsupported,jdk.crypto.ec,jdk.zipfs

FROM ghcr.io/oracle/oraclelinux:8-slim

LABEL "org.opencontainers.image.authors"="Ryan Eberhard <ryan.eberhard@oracle.com>, Russell Gold <russell.gold@oracle.com>" \
      "org.opencontainers.image.url"="https://github.com/oracle/weblogic-monitoring-exporter" \
      "org.opencontainers.image.source"="https://github.com/oracle/weblogic-monitoring-exporter" \
      "org.opencontainers.image.vendor"="Oracle Corporation" \
      "org.opencontainers.image.title"="Oracle WebLogic Monitoring Exporter" \
      "org.opencontainers.image.description"="Oracle WebLogic Monitoring Exporter" \
      "org.opencontainers.image.documentation"="https://github.com/oracle/weblogic-monitoring-exporter"

COPY --from=jre-build /jre jre

RUN set -eux; \
    microdnf -y update; \
    microdnf clean all; \
    for bin in /jre/bin/*; do \
        base="$(basename "$bin")"; \
        [ ! -e "/usr/bin/$base" ]; \
        alternatives --install "/usr/bin/$base" "$base" "$bin" 20000; \
    done; \
    java -Xshare:dump

COPY --from=build project/wls-exporter-sidecar/target/wls-exporter-sidecar.jar ./
COPY --from=build project/wls-exporter-sidecar/target/libs ./libs
COPY start_exporter.sh .

ENTRYPOINT ["sh", "start_exporter.sh"]

EXPOSE 8080
