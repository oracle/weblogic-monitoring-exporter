#!/bin/bash
# Copyright (c) 2021, 2022, Oracle and/or its affiliates.
# Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

curl -L -O https://github.com/oracle/weblogic-monitoring-exporter/releases/download/v${tag}/wls-exporter.war

if [ ! -z "$1" ]; then
  tmp_dir=$(mktemp -d -t ci-XXXXXXXXXX)
  echo "created $tmp_dir"
  cp $1 $tmp_dir/config.yml
  warDir=$PWD
  pushd $tmp_dir
  echo "in temp dir"
  zip $warDir/wls-exporter.war config.yml
  popd
#  rm -rf $tmp_dir
fi
