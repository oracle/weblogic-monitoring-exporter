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

# Install Java on top of the linux image
FROM ghcr.io/oracle/oraclelinux:8-slim as linux
WORKDIR /tmp

RUN set -eux; \
    microdnf -y install gzip tar openssl jq; \
    microdnf -y update; \
    microdnf clean all

ENV LANG="en_US.UTF-8" \
    JAVA_HOME="/usr/local/java" \
    PATH="/operator:$JAVA_HOME/bin:$PATH" \
    JAVA_URL="https://download.java.net/java/GA/jdk17.0.2/dfd4a8d0985749f896bed50d7138ee7f/8/GPL/openjdk-17.0.2_linux-x64_bin.tar.gz"

RUN set -eux; \
    curl -fL -o /jdk.tar.gz "$JAVA_URL"; \
    mkdir -p "$JAVA_HOME"; \
    tar --extract --file /jdk.tar.gz --directory "$JAVA_HOME" --strip-components 1; \
    rm /jdk.tar.gz; \
    mkdir /usr/java; \
    ln -sfT "$JAVA_HOME" /usr/java/default; \
    ln -sfT "$JAVA_HOME" /usr/java/latest; \
    rm -Rf "$JAVA_HOME/include" "$JAVA_HOME/jmods"; \
    rm -f "$JAVA_HOME/lib/src.zip"; \
    for bin in "$JAVA_HOME/bin/"*; do \
        base="$(basename "$bin")"; \
        [ ! -e "/usr/bin/$base" ]; \
        alternatives --install "/usr/bin/$base" "$base" "$bin" 20000; \
    done; \
    java -Xshare:dump

# Finally, copy the exporter sidecar and create the docker image
FROM linux as base

LABEL "org.opencontainers.image.authors"="Ryan Eberhard <ryan.eberhard@oracle.com>, Russell Gold <russell.gold@oracle.com>" \
      "org.opencontainers.image.url"="https://github.com/oracle/weblogic-monitoring-exporter" \
      "org.opencontainers.image.source"="https://github.com/oracle/weblogic-monitoring-exporter" \
      "org.opencontainers.image.vendor"="Oracle Corporation" \
      "org.opencontainers.image.title"="Oracle WebLogic Monitoring Exporter" \
      "org.opencontainers.image.description"="Oracle WebLogic Monitoring Exporter" \
      "org.opencontainers.image.documentation"="https://github.com/oracle/weblogic-monitoring-exporter"

COPY --from=build project/wls-exporter-sidecar/target/wls-exporter-sidecar.jar ./
COPY --from=build project/wls-exporter-sidecar/target/libs ./libs
COPY start_exporter.sh .

ENTRYPOINT ["sh", "start_exporter.sh"]

EXPOSE 8080
