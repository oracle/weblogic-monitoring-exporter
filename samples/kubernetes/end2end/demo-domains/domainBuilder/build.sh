#!/bin/bash
# Copyright 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
# Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.

set -e  # Exit immediately if a command exits with a non-zero status.

WDT_VERSION=0.24
MONITORING_EXPORTER_VERSION=1.1.1

CUR_DIR="$(dirname "$(readlink -f "$0")")" # get the absolute path of this file's folder
PRJ_ROOT=${CUR_DIR}/../../../../..
TMP_DIR=${CUR_DIR}/tmp

# Create two webapps: testwebapp and wls-exporter.
function createArchive() {
  mkdir -p ${TMP_DIR}/archive/wlsdeploy/applications

  echo 'Build the test webapp...'
  cd test-webapp && jar cvf testwebapp.war . && cd ..
  cp test-webapp/testwebapp.war ${TMP_DIR}/archive/wlsdeploy/applications/testwebapp.war

  echo "Download the metrics exporter webapp from ://github.com/oracle/weblogic-monitoring-exporter/releases/download/v${MONITORING_EXPORTER_VERSION}/get${MONITORING_EXPORTER_VERSION}.sh..."

  cd $PRJ_ROOT
  curl -O -L https://github.com/oracle/weblogic-monitoring-exporter/releases/download/v${MONITORING_EXPORTER_VERSION}/get${MONITORING_EXPORTER_VERSION}.sh
  chmod 777 get${MONITORING_EXPORTER_VERSION}.sh
  bash get${MONITORING_EXPORTER_VERSION}.sh ${CUR_DIR}/../../dashboard/exporter-config.yaml
  echo 'Monitoring Exporter web application is built'
  
  cd $CUR_DIR 
  cp $PRJ_ROOT/wls-exporter.war \
     ${TMP_DIR}/archive/wlsdeploy/applications/wls-exporter.war

  echo 'Build the WDT archive...'
  jar cvf ${TMP_DIR}/archive.zip  -C ${TMP_DIR}/archive wlsdeploy
  rm -rf ${TMP_DIR}/archive
}

function cleanTmpDir() {
  rm -rf ${CUR_DIR}/test-webapp/testwebapp.war
  rm -rf ${PRJ_ROOT}/wls-exporter.war
  rm -rf ${TMP_DIR}
}

function buildImage() {
  cp ${CUR_DIR}/scripts/* ${TMP_DIR}
  echo "Update domain.properties with cmdline arguments..."
  sed -i "s/^DOMAIN_NAME.*/DOMAIN_NAME=$1/g" ${TMP_DIR}/domain.properties
  sed -i "s/^ADMIN_USER.*/ADMIN_USER=$2/g" ${TMP_DIR}/domain.properties
  sed -i "s/^ADMIN_PWD.*/ADMIN_PWD=$3/g" ${TMP_DIR}/domain.properties
  sed -i "s/^MYSQL_USER.*/MYSQL_USER=$4/g" ${TMP_DIR}/domain.properties
  sed -i "s/^MYSQL_PWD.*/MYSQL_PWD=$5/g" ${TMP_DIR}/domain.properties

  echo 'Download the wdt zip...'
  wget -P ${TMP_DIR} \
    https://github.com/oracle/weblogic-deploy-tooling/releases/download/weblogic-deploy-tooling-${WDT_VERSION}/weblogic-deploy.zip

  imageName=$1-image:1.0
  echo "Build the domain image $imageName..."
  docker build $CUR_DIR --force-rm -t $imageName
}

if [ "$#" != 5 ] ; then
  echo "usage: $0 domainName adminUser adminPwd mysqlUser mysqlPwd"
  exit 1 
fi

cleanTmpDir
createArchive
buildImage $@
cleanTmpDir
