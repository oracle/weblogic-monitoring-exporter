#   Copyright (c) 2021, 2025, Oracle and/or its affiliates.
#   Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

FROM ghcr.io/oracle/oraclelinux:9-slim AS jre-build

ENV JAVA_URL_X64="https://download.oracle.com/java/21/latest/jdk-21_linux-x64_bin.tar.gz"
ENV JAVA_URL_AARCH64="https://download.oracle.com/java/21/latest/jdk-21_linux-aarch64_bin.tar.gz"

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
    /jdk/bin/jlink --verbose --compress 2 --strip-java-debug-attributes --no-header-files --no-man-pages --output custom-jre --add-modules java.base,java.logging,java.desktop,java.instrument,java.management,java.naming,java.net.http,java.security.jgss,java.sql,jdk.attach,jdk.jdi,jdk.jfr,jdk.management,jdk.management.agent,jdk.management.jfr,jdk.net,jdk.unsupported,jdk.crypto.cryptoki,jdk.crypto.ec,jdk.zipfs,jdk.jcmd

FROM scratch

LABEL "org.opencontainers.image.authors"="Ryan Eberhard <ryan.eberhard@oracle.com>, Russell Gold <russell.gold@oracle.com>" \
      "org.opencontainers.image.url"="https://github.com/oracle/weblogic-monitoring-exporter" \
      "org.opencontainers.image.source"="https://github.com/oracle/weblogic-monitoring-exporter" \
      "org.opencontainers.image.vendor"="Oracle Corporation" \
      "org.opencontainers.image.title"="Oracle WebLogic Monitoring Exporter" \
      "org.opencontainers.image.description"="Oracle WebLogic Monitoring Exporter" \
      "org.opencontainers.image.documentation"="https://github.com/oracle/weblogic-monitoring-exporter"

ENV LANG="en_US.UTF-8"

COPY --from=jre-build custom-jre /opt/java
COPY --from=jre-build /lib64/libz.so.1 /lib64/libz.so.1
COPY --from=jre-build /lib64/libpthread.so.0 /lib64/libpthread.so.0
COPY --from=jre-build /lib64/libdl.so.2 /lib64/libdl.so.2
COPY --from=jre-build /lib64/libc.so.6 /lib64/libc.so.6
COPY --from=jre-build /lib64/libpthread.so.0 /lib64/libpthread.so.0

COPY wls-exporter-sidecar/target/wls-exporter-sidecar.jar /app/wls-exporter-sidecar.jar
COPY wls-exporter-sidecar/target/libs /app/libs

ENTRYPOINT ["/opt/java/bin/java", "$JAVA_OPTS", "-jar", "/app/wls-exporter-sidecar.jar"]

EXPOSE 8080
