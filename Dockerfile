#   Copyright (c) 2021, 2022, Oracle and/or its affiliates.
#   Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

# First layer: dependencies for the project, cached in the /root/.m2 directory
FROM maven:3-openjdk-18-slim as m2repo
ARG MAVEN_OPTS

WORKDIR /project/
COPY pom.xml .
COPY build-helper-mojo/pom.xml build-helper-mojo/
COPY wls-exporter-core/pom.xml wls-exporter-core/
COPY wls-exporter-sidecar/pom.xml wls-exporter-sidecar/

RUN mvn -B -e -C org.apache.maven.plugins:maven-dependency-plugin:3.1.2:go-offline -Ddocker-build

# Now build the project on top of that first layer
FROM maven:3-openjdk-18-slim as build
ARG MAVEN_OPTS

WORKDIR /project/
COPY --from=m2repo /root/.m2 /root/.m2
COPY pom.xml .
COPY build-helper-mojo/ build-helper-mojo/
COPY wls-exporter-core/ wls-exporter-core/
COPY wls-exporter-sidecar/ wls-exporter-sidecar/

RUN set -eux; \
    mvn -B -e -C install -Ddocker-build -DskipTests=true; \
    $JAVA_HOME/bin/jlink --verbose --compress 2 --strip-java-debug-attributes --no-header-files --no-man-pages --output /jre --add-modules java.base,java.logging,java.desktop,java.instrument,java.management,java.naming,java.net.http,java.security.jgss,java.sql,jdk.attach,jdk.jdi,jdk.jfr,jdk.management,jdk.management.jfr,jdk.net,jdk.unsupported,jdk.crypto.cryptoki,jdk.crypto.ec,jdk.zipfs

FROM ghcr.io/oracle/oraclelinux:8-slim

LABEL "org.opencontainers.image.authors"="Ryan Eberhard <ryan.eberhard@oracle.com>, Russell Gold <russell.gold@oracle.com>" \
      "org.opencontainers.image.url"="https://github.com/oracle/weblogic-monitoring-exporter" \
      "org.opencontainers.image.source"="https://github.com/oracle/weblogic-monitoring-exporter" \
      "org.opencontainers.image.vendor"="Oracle Corporation" \
      "org.opencontainers.image.title"="Oracle WebLogic Monitoring Exporter" \
      "org.opencontainers.image.description"="Oracle WebLogic Monitoring Exporter" \
      "org.opencontainers.image.documentation"="https://github.com/oracle/weblogic-monitoring-exporter"

COPY --from=build /jre jre

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