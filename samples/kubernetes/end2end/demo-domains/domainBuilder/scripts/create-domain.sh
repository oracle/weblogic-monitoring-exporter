#!/bin/bash
# Copyright 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
# Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.

set -exu

WDT_HOME="/u01"
DOMAIN_PARENT="/u01/oracle/user-projects/domains"

cd $WDT_HOME && \
jar xf ${SCRIPTS_DIR}/weblogic-deploy.zip && \
chmod +x weblogic-deploy/bin/*.sh && \
mkdir -p $DOMAIN_PARENT && \

${WDT_HOME}/weblogic-deploy/bin/createDomain.sh \
    -oracle_home $ORACLE_HOME \
    -java_home $JAVA_HOME \
    -domain_parent $DOMAIN_PARENT \
    -domain_type WLS \
    -model_file ${SCRIPTS_DIR}/simple-topology.yaml \
    -archive_file ${SCRIPTS_DIR}/archive.zip \
    -variable_file ${SCRIPTS_DIR}/domain.properties
