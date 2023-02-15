#   Copyright (c) 2021, 2023, Oracle and/or its affiliates.
#   Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

FROM ghcr.io/oracle/oraclelinux:9-slim AS jre-build

ENV JAVA_URL="https://download.java.net/java/GA/jdk19.0.2/fdb695a9d9064ad6b064dc6df578380c/7/GPL/openjdk-19.0.2_linux-x64_bin.tar.gz"

RUN set -eux; \
    microdnf -y install gzip tar; \
    curl -fL -o /jdk.tar.gz "$JAVA_URL"; \
    mkdir -p /jdk; \
    tar --extract --file /jdk.tar.gz --directory /jdk --strip-components 1; \
    /jdk/bin/jlink --verbose --compress 2 --strip-java-debug-attributes --no-header-files --no-man-pages --output jre --add-modules java.base,java.desktop,java.instrument,java.management,java.naming,java.net.http,java.sql,jdk.attach,jdk.jdi,jdk.unsupported,jdk.crypto.ec,jdk.zipfs

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
    microdnf -y install jq; \
    microdnf clean all; \
    for bin in /jre/bin/*; do \
        base="$(basename "$bin")"; \
        [ ! -e "/usr/bin/$base" ]; \
        alternatives --install "/usr/bin/$base" "$base" "$bin" 20000; \
    done; \
    java -Xshare:dump; \
    useradd -M -s /bin/bash -g root -u 1000 oracle

USER oracle

COPY --chown=oracle:root wls-exporter-sidecar/target/wls-exporter-sidecar.jar ./
COPY --chown=oracle:root wls-exporter-sidecar/target/libs ./libs
COPY --chown=oracle:root start_exporter.sh .

CMD ["/start_exporter.sh"]

EXPOSE 8080