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

terminate_flag=0
trap 'echo Terminating WME; terminate_flag=1' TERM INT

java $JAVA_OPTS -jar wls-exporter-sidecar.jar &
wme_pid=$!

wait $wme_pid
exit_code=$?

# If the shell receives a SIGTERM, it will not be propagated to children but 'wait' will exit
# Send SIGTERM to children (assuming SIGTERM triggers children shutdown) and wait for children to exit
if [[ ${terminate_flag} == 1 ]]; then
  kill $wme_pid
  wait $wme_pid
  exit_code=$?
fi

echo 'WME exit code: '$exit_code
exit 0