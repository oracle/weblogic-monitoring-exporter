#   Copyright (c) 2021, Oracle and/or its affiliates.
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

RUN mvn -B -e -C install -Ddocker-build
#RUN mvn -X install -Ddocker-build -DskipTests=true

# Finally, copy the exporter sidecar and create the docker image
FROM openjdk:11-oracle
WORKDIR /tmp

COPY --from=build project/wls-exporter-sidecar/target/wls-exporter-sidecar.jar ./
COPY --from=build project/wls-exporter-sidecar/target/libs ./libs

CMD ["java", "-jar", "wls-exporter-sidecar.jar"]

EXPOSE 8080
