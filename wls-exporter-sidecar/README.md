# Operator Sidecar

This module creates a sidecar implementation of the exporter, which runs in a separate process. Because it uses
Helidon, it requires JDK11 or later. Building on JDK8 will not build the sidecar.

## Build and run

With JDK11+
```mvn clean install
java -jar wls-exporter-sidecar/target/wls-exporter-sidecar.jar
``` 

This will start the exporter on port 8080, and expect to find a WebLogic Server instance locally, listening on port 7001.

## Configure the exporter

In this build, the exporter does not have a landing page. Configuration is done by sending a PUT request to the path `/configuration`. 
You can do this with `curl`

```
curl -X PUT -i -u myname:mypassword \
    -H "content-type: application/yaml" \
    --data-binary "@<path to yaml>" \
    http://localhost:8080/configuration
``` 

where you should replace `myname` and `mypassword` with the credentials expected by WebLogic Server for its REST API,
and `<path to yaml>` is the relative path to the configuration to use.

## Access the metrics

Once the exporter is configured, a GET to `http://localhost:8080/metrics` will return the current metrics.

## Copyright

 Copyright &copy; 2021, Oracle and/or its affiliates.
 Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
