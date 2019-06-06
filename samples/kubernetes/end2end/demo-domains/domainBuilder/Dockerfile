
FROM container-registry.oracle.com/middleware/weblogic:12.2.1.3

ENV SCRIPTS_DIR="/tmp/scripts/"

RUN mkdir -p $SCRIPTS_DIR
COPY tmp/* $SCRIPTS_DIR
RUN $SCRIPTS_DIR/create-domain.sh && \
    rm -rf ${SCRIPTS_DIR}

WORKDIR $ORACLE_HOME

