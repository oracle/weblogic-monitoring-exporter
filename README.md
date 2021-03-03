WebLogic Monitoring Exporter
=====

[![Build Status](https://travis-ci.org/oracle/weblogic-monitoring-exporter.svg?branch=master)](https://travis-ci.org/oracle/weblogic-monitoring-exporter)

The WebLogic Monitoring Exporter uses the [WLS RESTful Management API](https://docs.oracle.com/middleware/1221/wls/WLRUR/overview.htm#WLRUR111) to scrape runtime information and then exports [Prometheus](http://prometheus.io)-compatible metrics.
It is available both as a [web application](#web-application) to be deployed to the WebLogic Server (WLS) instance, version 12.2.1 or later, from which you want to get metrics, 
and also as a [sidecar](#sidecar), which runs as its own process, and which is configured to contact a WLS instance.

## Configuration
Here is an example `yaml` configuration:
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
| `query_sync` | Optional. Configuration for a [service](config_coordinator/README.md) which coordinates updates to the query configuration. |
| `query_sync.url` | The URL of the service. Required if this section is present. |
| `query_sync.interval` | The interval, in seconds, at which the service will be queried. Defaults to 10. |
| `metricsNameSnakeCase` | If true, metrics names will be converted to snake case. Defaults to false. |
| `domainQualifier` | If true, the domain name will be included as a qualifier for all metrics. Defaults to false. |
| `restPort` | Optional. Overrides the port on which the exporter should contact the REST API. Needed if the exporter cannot find the REST API. |

The `query` field is more complex. Each query consists of a hierarchy of the [MBeans](https://docs.oracle.com/middleware/1221/wls/WLMBR/core/index.html), starting relative to `ServerRuntimes`.
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


# Web Application

## Setting the configuration

The Web Application has a main landing page which displays the current [configuration](#configuration) and allows
a user to change it, either by uploading a replacement or an addition to the queries specified with the current one.
Metrics will then be available from `<application-root>/metrics`.


## Downloading the release

You can find all the releases on the [Releases page](https://github.com/oracle/weblogic-monitoring-exporter/releases).

To download the web application `wls-exporter.war` and put your configuration file into the WAR, download the `getXXX.sh` script, which is provided with each release and also can be downloaded from the Releases page, and then run:

```
bash getXXX.sh <your-config-file>
```

## Building from source

Use `mvn install` to build the web application. This will create `wls-exporter-<version>`, where _version_
is the Maven-assigned version number.

Adding `-Dconfiguration=<some-config-file>` will insert the specified configuration as its default and remove
the version number to simplify deployment to WebLogic Server.

# Sidecar

## Build and run

The sidecar implementation must be built with JDK11 or later. Building the project with JDK8 will skip the sidecar module.

After building, execute:
```
java -jar wls-exporter-sidecar/target/wls-exporter-sidecar.jar
```

This will start the exporter on port 8080, and expect to find a WebLogic Server instance locally, listening on port 7001.

That may be changed by specifying parameters on the command line:

Setting | Default | Property
------------ | ------------- | -------------
Domain name | (use WLS definition) | DOMAIN
Exporter port | 8080 | EXPORTER_PORT
WebLogic host | localhost | WLS_HOST
WebLogic port | 7001 | WLS_PORT
Use https | false | WLS_SECURE

## Configure the exporter

The sidecar does not have a landing page. Configuration is done by sending a PUT request to the path `/configuration`. 
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

Once the exporter is configured, a GET to `http://localhost:8080/metrics` (or whatever port was chosen) will return the current metrics.

## Building a docker image

If docker is installed, running 

```
docker build . -t <image-name>
```

will build the project and create a docker image with the specified name. It is not necessary even 
to do the Maven build first, as that will
happen as part of creating the image. When running behind a firewall, it is necessary to specify a value for 
the MAVEN_OPTS variable on the command line. For example

```
docker build . --build-arg MAVEN_OPTS="-Dhttps.proxyHost=www-proxy -Dhttps.proxyPort=80" -t <image-name>
```

To allow the Maven image to download the dependencies. 


 
## Copyright

 Copyright &copy; 2017, 2021, Oracle and/or its affiliates.
 Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
