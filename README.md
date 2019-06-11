WebLogic Monitoring Exporter
=====

[![Build Status](https://travis-ci.org/oracle/weblogic-monitoring-exporter.svg?branch=master)](https://travis-ci.org/oracle/weblogic-monitoring-exporter)

The WebLogic Monitoring Exporter uses the [WLS RESTful Management API](https://docs.oracle.com/middleware/1221/wls/WLRUR/overview.htm#WLRUR111) to scrape runtime information, and then exports [Prometheus](http://prometheus.io)-compatible metrics.
It is deployed as a web application in a WebLogic Server (WLS) instance, version 12.2.1 or higher, typically, in the instance from which you want to get metrics.

## Building

Use `mvn install` to build the exporter servlets.

Then `cd webapp & mvn package` to build the web application `wls-exporter.war`, which can then be deployed to WLS.

Adding `-Dconfiguration=<some-config-file>` will create a web application with the specified configuration as its default.

## Configuration
Here is an example `yaml` configuration:
```
query_sync:
  url: http://coordinator:8999/
  interval: 5
metricsNameSnakeCase: true
domainQualifier: true
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
Note that there are two parts to the configuration. The optional top portion defines general processing rules:

| Name | Description |
| --- | --- |
| `query_sync` | Optional. Configuration for a [service](config_coordinator/README.md) which coordinates updates to the query configuration. |
| `query_sync.url` | The URL of the service. Required if this section is present. |
| `query_sync.interval` | The interval, in seconds, at which the service will be queried. Defaults to 10. |
| `metricsNameSnakeCase` | If true, metrics names will be converted to snake case. Defaults to false. |
| `domainQualifier` | If true, the domain name will be included as a qualifier for all metrics. Defaults to false. |
| `restPort` | Optional. Overrides the port on which the exporter should contact the REST API. Needed when behind a load balancer. |

The `query` field is more complex. Each query consists of a hierarchy of the [MBeans](https://docs.oracle.com/middleware/1221/wls/WLMBR/core/index.html), starting relative to `ServerRuntimes`.
Within each section, there are a number of options:

| Name | Description |
| --- | --- |
| `key` | The name of the attribute to use as a key for qualifiers in the output. |
| `keyName` | The name to use for the key in the qualifier; defaults to the name of the attribute. |
| `prefix` | A prefix to use for all metrics gathered from the current level. |
| `values` | The attributes for which metrics are to be output. If not specified and a prefix is defined, all values on the MBean will be selected. |
| `type` | A filter for subtypes. If specified, only those objects whose `type` attribute matches will be collected. |

Note that all fields other than the above, will be interpreted as collections of values.

In the above example, the presumed underlying data structure is:
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
however, if the metrics request is made via a load balancer or Kubernetes NodePort service, the port to which the
original request was made might not be the same as the instance's HTTP port. In such a case, the configuration should
include the `restPort` configuration to tell the exporter which port to use.

## Copyright

 Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
