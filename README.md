WebLogic Monitoring Exporter
=====

[![Build Status](https://travis-ci.org/oracle/weblogic-monitoring-exporter.svg?branch=master)](https://travis-ci.org/oracle/weblogic-monitoring-exporter)

The WebLogic Monitoring Exporter is a [Prometheus](http://prometheus.io)-compatible exporter of metrics from 
WebLogic Server (WLS) instances, which it obtains by using the 
[WLS RESTful Management API](https://docs.oracle.com/middleware/12213/wls/WLRUR/overview.htm#WLRUR111), available in version 12.2.1 or later. 
Metrics are selected using a [YAML configuration file](#Configuration).

The exporter is available in two forms:
 - A [web application](#web-application) that you deploy to the server from which metrics are to be extracted. 
 You may include a configuration file directly in the WAR file, and you may temporarily modify the configuration in a 
 running system by using a web form. If a [coordination configurator](config_coordinator/README.md) is running and configured, 
 that temporary configuration will be sent to all servers configured to use it.
 
 - A [separate process](#sidecar) that is run alongside a server instance. You supply the configuration to such a
process with a PUT command, as described below. [WebLogic Server Kubernetes Operator](https://github.com/oracle/weblogic-kubernetes-operator/) versions 3.2 and later have special support for the exporter in this form.


## Configuration
Here is an example `yaml` file configuration:
```
query_sync:
  url: http://coordinator:8999/
  interval: 5
metricsNameSnakeCase: true
domainQualifier: true
restPort: 7001
queries:
- key: name
  keyName: server
  applicationRuntimes:
    key: name
    keyName: app
    componentRuntimes:
      type: WebAppComponentRuntime
      prefix: webapp_config_
      key: name
      values: [deploymentState, contextRoot, sourceInfo, openSessionsHighCount]
      servlets:
        prefix: weblogic_servlet_
        key: servletName
        values: invocationTotalCount
```
Note that there are two parts to the configuration. The optional top portion defines general processing rules.

| Name | Description |
| --- | --- |
| `query_sync` | Optional, used in the web application only. Configuration for a [service](config_coordinator/README.md) which coordinates updates to the query configuration. |
| `query_sync.url` | The URL of the service. Required if this section is present. |
| `query_sync.interval` | The interval, in seconds, at which the service will be queried. Defaults to 10. |
| `metricsNameSnakeCase` | If true, metrics names will be converted to snake case. Defaults to false. |
| `domainQualifier` | If true, the domain name will be included as a qualifier for all metrics. Defaults to false. |
| `restPort` | Optional, used in the web application only. Overrides the port on which the exporter should contact the REST API. Needed if the exporter cannot find the REST API. The most common case is running on a system with the administration port enabled. In that case, you must specify the administration port in this field and access the exporter by using the SSL port. |

The `query` field is more complex. Each query consists of a hierarchy of the [MBeans](https://docs.oracle.com/middleware/12213/wls/WLMBR/core/index.html), starting relative to `ServerRuntimes`.
Within each section, there are a number of options:

| Name | Description |
| --- | --- |
| `key` | The name of the attribute to use as a key for qualifiers in the output. |
| `keyName` | The name to use for the key in the qualifier; defaults to the name of the attribute. |
| `prefix` | A prefix to use for all the metrics gathered from the current level. |
| `values` | The attributes for which metrics are to be output. If not specified and a prefix is defined, all values on the MBean will be selected. |
| `type` | A filter for subtypes. If specified, only those objects whose `type` attribute matches will be collected. |

Note that all fields other than the above, will be interpreted as collections of values.

In the preceding example, the presumed underlying data structure is:
```
+---------------+   applicationRuntimes     
| ServerRuntime |-----------+                 
+---------------+           |
                            v
            +--------------------+                       +------------------+
            | ApplicationRuntime |   componentRuntimes   | ComponentRuntime |
            |--------------------| --------------------> |------------------|
            | + name             |                       | + type           |
            +--------------------|                       +------------------+
                                                                   ^
                                                                  / \
                                                                   |
     +------------------------+                    +-------------------------+
     |        Servlet         |       servlets     | WebAppComponentRuntime  |
     |------------------------| <----------------- |-------------------------|
     | + servletName          |                    | + name                  |
     | + invocationTotalCount |                    | + contextRoot           |
     +------------------------+                    | + deploymentState       |
                                                   | + sourceInfo            |
                                                   | + openSessionsHighCount |
                                                   +-------------------------+
```                                                             
 The above configuration will then produce metrics such as:

```
webapp_config_deployment_state{domain="mydomain",server="myserver",app="myapp",name="aWebApp"}                                                             
webapp_config_open_sessions_high_count{domain="mydomain",server="myserver",app="myapp",name="aWebApp"}
weblogic_servlet_invocation_total_count{domain="mydomain",server="myserver",app="myapp",name="aWebApp",servletName="servlet1"}                                                             
weblogic_servlet_invocation_total_count{domain="mydomain",server="myserver",app="myapp",name="aWebApp",servletName="simpleServlet"}                                                             
```                                                             
Note that no help or type text is currently produced, as the REST API has no access to the underlying MBean info.


## Self-Monitoring

The exporter produces metrics for monitoring its own performance:

- `wls_scrape_mbeans_count_total` reports the number of metrics scraped.
- `wls_scrape_duration_seconds` reports the time required to do the scrape.
- `wls_scrape_cpu_seconds` reports the CPU time used during the scrape.


## Access to the REST API

The exporter must be able to contact the REST API of the WLS instance on which it is deployed. It does so by using
the primary host address of its server and the port on which the request for metrics was made. Usually, that will work;
however, if the metrics request is made using a load balancer or Kubernetes NodePort service, the port to which the
original request was made might not be the same as the instance's HTTP port. In such a case, the configuration should
include the `restPort` configuration to tell the exporter which port to use.


## Samples

* [Deployment-based sample](samples/kubernetes/deployments): This sample helps you set up and run Prometheus and Grafana with the WebLogic Monitoring Exporter. Deployment YAML files are provided to install Prometheus and Grafana.

* [Chart-based sample](samples/kubernetes/end2end): This is an end-to-end sample that shows you the steps to set up monitoring for WebLogic domains using Prometheus and Grafana. Prometheus and Grafana are installed with Helm charts.


# Web application

One way to use the exporter is by creating a WAR file with a default configuration and deploying it to a WebLogic Server instance.

## Setting the configuration

The web application has a main landing page, which displays the current [configuration](#configuration) and allows
you to change it, either by uploading a replacement or an addition to the queries specified with the current one.
Metrics will then be available from `<application-root>/metrics`.


## Downloading the release

You can find all the exporter releases on the [Releases page](https://github.com/oracle/weblogic-monitoring-exporter/releases/).

To download the web application `wls-exporter.war` file and put your configuration file into the WAR, download the `getXXX.sh` script, which is provided with each release and also can be downloaded from the Releases page, and then run:

```
bash getXXX.sh <your-config-file>
```

## Building from source

Use `mvn install` to build the web application. This will create `wls-exporter-<version>`, where _version_
is the Maven-assigned version number.

Adding `-Dconfiguration=<some-config-file>` will insert the specified configuration as its default and remove
the version number to simplify deployment to WebLogic Server.

# Sidecar

The sidecar is a standalone process that runs the exporter.

## Build and run with Maven

There are two ways to build the sidecar implementation. The first is with Maven, using the same `mvn install` command 
specified [above](#building-from-source). Note that this requires JDK11 or later; building the project with JDK8 will 
skip the sidecar module. The alternative is to [build with Docker](#building-a-docker-image) 

After building, run:
```
java -jar wls-exporter-sidecar/target/wls-exporter-sidecar.jar
```

This will start the exporter on port 8080, and it will expect to find a local WebLogic Server instance, listening on port 7001.

You can make changes by specifying parameters on the command line:

Setting | Default | Property
------------ | ------------- | -------------
Domain name | (use WLS definition) | `DOMAIN`
Exporter port | `8080` | `EXPORTER_PORT`
WebLogic host name | `localhost` | `WLS_HOST`
WebLogic port | `7001` | `WLS_PORT`
Use https | `false` | `WLS_SECURE`

## Configure the exporter

You configure the sidecar by sending a PUT request to the path `/configuration`. 
You can do this with `curl`; that is how the operator does it. 

```
curl -X PUT -i -u myname:mypassword \
    -H "content-type: application/yaml" \
    --data-binary "@<path to yaml>" \
    http://localhost:8080/configuration
``` 

Replace `myname` and `mypassword` with the credentials expected by WebLogic Server for its REST API,
and `<path to yaml>` with the relative path to the configuration to use.

## Access the metrics

After the exporter is configured, a GET to `http://localhost:8080/metrics` (or whatever port was chosen) will return the current metrics.

## Building a Docker image

If Docker is installed, you can build the image with the following command.

```
docker build . -t <image-name>
```

This will build the project and create a Docker image with the specified name. It is not necessary even 
to do the Maven build first, as that will happen as part of creating the image. When running behind a firewall, 
it is necessary to specify a value for the `MAVEN_OPTS` and `https_proxy` variables on the command line. For example:

```
docker build . --build-arg MAVEN_OPTS="-Dhttps.proxyHost=www-proxy -Dhttps.proxyPort=80" \
               --build-arg https_proxy=www-proxy:80 \
               -t <image-name>
```

This allows Docker to download the dependencies. 


 
## Copyright

 Copyright &copy; 2017, 2021, Oracle and/or its affiliates.
 Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
