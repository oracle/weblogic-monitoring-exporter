# Start from a Debian image with the desired version of Go installed
# and a workspace (GOPATH) configured at /go.
FROM golang:1.9.2

# Copy the local files to the container's workspace.
COPY . /go/src/github.com/oracle/config_coordinator

# Build the config_coordinator command inside the container.
RUN go install github.com/oracle/config_coordinator

# Run the config_coordinator by default when the container starts.
ENTRYPOINT ["/go/bin/config_coordinator"]

# Document that the service listens on port 8999.
EXPOSE 8999