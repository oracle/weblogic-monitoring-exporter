#   Copyright (c) 2021, 2025, Oracle and/or its affiliates.
#   Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

FROM ghcr.io/oracle/oraclelinux:9-slim AS jre-build

ENV JAVA_URL_X64="https://download.oracle.com/java/25/latest/jdk-25_linux-x64_bin.tar.gz"
ENV JAVA_URL_AARCH64="https://download.oracle.com/java/25/latest/jdk-25_linux-aarch64_bin.tar.gz"

RUN set -eux; \
    microdnf -y install gzip tar; \
    MACHINE_TYPE=`uname -m`; \
    if [ ${MACHINE_TYPE} == 'x86_64' ]; then \
      JAVA_URL=$JAVA_URL_X64; \
    else \
      JAVA_URL=$JAVA_URL_AARCH64; \
    fi; \
    curl -fL -o jdk.tar.gz "$JAVA_URL"; \
    mkdir -p /jdk; \
    tar --extract --file jdk.tar.gz --directory /jdk --strip-components 1; \
    /jdk/bin/jlink --verbose --compress 2 --strip-java-debug-attributes --no-header-files --no-man-pages --output jre --add-modules java.base,java.logging,java.instrument,java.management,java.naming,java.net.http,java.security.jgss,java.sql,jdk.attach,jdk.jdi,jdk.jfr,jdk.management,jdk.management.agent,jdk.management.jfr,jdk.net,jdk.unsupported,jdk.crypto.cryptoki,jdk.crypto.ec,jdk.zipfs,jdk.jcmd

FROM ghcr.io/oracle/oraclelinux:9-slim

LABEL "org.opencontainers.image.authors"="Ryan Eberhard <ryan.eberhard@oracle.com>, Russell Gold <russell.gold@oracle.com>" \
      "org.opencontainers.image.url"="https://github.com/oracle/weblogic-monitoring-exporter" \
      "org.opencontainers.image.source"="https://github.com/oracle/weblogic-monitoring-exporter" \
      "org.opencontainers.image.vendor"="Oracle Corporation" \
      "org.opencontainers.image.title"="Oracle WebLogic Monitoring Exporter" \
      "org.opencontainers.image.description"="Oracle WebLogic Monitoring Exporter" \
      "org.opencontainers.image.documentation"="https://github.com/oracle/weblogic-monitoring-exporter"

ENV LANG="en_US.UTF-8"

COPY --from=jre-build /jre jre

# Install Java and make the operator run with a non-root user id (1000 is the `oracle` user)
RUN set -eux; \
    microdnf -y update; \
    microdnf clean all; \
    for bin in /jre/bin/*; do \
        base="$(basename "$bin")"; \
        [ ! -e "/usr/bin/$base" ]; \
        alternatives --install "/usr/bin/$base" "$base" "$bin" 20000; \
    done; \
    java -Xshare:dump; \
    useradd -M -s /bin/bash -g root -u 1000 oracle

USER oracle

COPY wls-exporter-sidecar/target/wls-exporter-sidecar.jar /app/wls-exporter-sidecar.jar
COPY wls-exporter-sidecar/target/libs /app/libs

ENTRYPOINT ["/jre/bin/java", "-jar", "/app/wls-exporter-sidecar.jar"]

EXPOSE 8080
