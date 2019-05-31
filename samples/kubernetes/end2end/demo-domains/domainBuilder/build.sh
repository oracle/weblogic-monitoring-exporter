#!/bin/bash
# Copyright 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
# Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
set -e 

MYDIR="$(dirname "$(readlink -f "$0")")" # get the absolute path of this file's folder
PRJ_ROOT=$MYDIR/../../../../..

# To build the archive which contains binaries and scripts etc.
# Refer to WDT doc about the archive format: https://github.com/oracle/weblogic-deploy-tooling#simple-example
function createArchive() {
  mkdir -p ${MYDIR}/archive/wlsdeploy/applications
  mkdir -p ${MYDIR}/archive/wlsdeploy/domainLibraries

  echo 'Build the test webapp...'
  cd test-webapp && mvn clean install && cd ..
  cp test-webapp/target/testwebapp.war ${MYDIR}/archive/wlsdeploy/applications/testwebapp.war

  echo 'Build the metrics exporter...'
  cd $PRJ_ROOT
  mvn clean install
  cd webapp
  mvn clean package -Dconfiguration=$MYDIR/../../dashboard/exporter-config.yaml
  cd $MYDIR 
  cp $PRJ_ROOT/webapp/target/wls-exporter.war \
     ${MYDIR}/archive/wlsdeploy/applications/wls-exporter.war

  echo 'Build the WDT archive...'
  jar cvf ${MYDIR}/archive.zip  -C ${MYDIR}/archive wlsdeploy
}

function cleanTmpDir() {
  rm -rf ${MYDIR}/archive
  rm -rf ${MYDIR}/test-webapp/target
  rm -f  ${MYDIR}/archive.zip
  rm -rf $PRJ_ROOT/webapp/target
  rm -f $MYDIR/weblogic-deploy.zip
}

function buildImage() {
  if [ ! -e $MYDIR/weblogic-deploy.zip ] ; then
    echo "weblogic-deploy.zip does not exist. Downloading it from github."  
    wget -P $MYDIR https://github.com/oracle/weblogic-deploy-tooling/releases/download/weblogic-deploy-tooling-0.11/weblogic-deploy.zip
    echo "download complete"
  fi

  imageName=$1-image:1.0
  echo "build image $imageName"
  docker build --build-arg ARG_DOMAIN_NAME=$1  --build-arg ADMIN_USER=$2 \
   --build-arg ADMIN_PWD=$3 $MYDIR --force-rm -t $imageName
}

if [ "$#" != 3 ] ; then
  echo "usage: $0 domainName adminUser adminPwd"
  exit 1 
fi

cleanTmpDir
createArchive
buildImage $@
cleanTmpDir
