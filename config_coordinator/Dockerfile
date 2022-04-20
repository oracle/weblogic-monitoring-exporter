# Copyright 2019, 2022, Oracle and/or its affiliates.
# Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

# Start from a Debian image with the desired version of Go installed
# and a workspace (GOPATH) configured at /go.
FROM golang:1.18.1

# Copy the local files to the container's workspace.
COPY . /go/src/github.com/oracle/config_coordinator

# Build the config_coordinator command inside the container.
RUN go install /go/src/github.com/oracle/config_coordinator/config_coordinator.go

# Run the config_coordinator by default when the container starts.
ENTRYPOINT ["/go/bin/config_coordinator"]

# Document that the service listens on port 8999.
EXPOSE 8999
