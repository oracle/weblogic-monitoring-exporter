#!/bin/bash
# Copyright (c) 2021, 2025, Oracle and/or its affiliates.
# Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

cleanup() {
  echo "Suppressing signal to allow container to exit cleanly."
  exit 0
}

trap cleanup SIGTERM SIGKILL

exitMessage() {
  echo "Exiting container for WebLogic monitoring exporter."
}

trap exitMessage EXIT

java $JAVA_OPTS -jar wls-exporter-sidecar.jar