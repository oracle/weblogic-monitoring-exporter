WLS Prometheus Exporter
=====

WLS REST to Prometheus exporter.

A Collector that uses the [WLS proprietary REST API](https://docs.oracle.com/middleware/1221/wls/WLRUR/overview.htm#WLRUR111) to scrape runtime information. It is intended to be deployed as a
web application in a WLS instance, version 12.2.1 or higher. Typically, this is deployed to the instance you want to scrape.

## Building

`mvn install` to build the exporter and servlets. 

Then `cd webapp & mvn package` to build the webapp `wls-exporter.war`, which can then be deployed to WLS.

Adding `-Dconfiguration=<some-config-file>` will create a webapp with the specified configuration as its default.

## Configuration
Here is an example `yaml` configuration:
```
query_sync:
  url: http://coordinator:8999/
  interval: 5
metricsNameSnakeCase: true
queries:
- applicationRuntimes:
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
| query_sync | Configuration for a [service](config_coordinator/README.md) which coordinates updates to the query configuration. Optional. |
| query_sync.url | The url of the service. Required if this section is present. |
| query_sync.interval | The interval in seconds at which the service will be queries. Defaults to 10. |
| metricsNameSnakeCase | If true, metrics names will be converted to snake case. Defaults to false |

The `query` field is more complex. Each query consists of a hierarchy of the mbeans, starting relative to `ServerRuntimes`.
Within each section, there are a number of options:

| Name | Description |
| --- | --- |
| key | the name of the attribute to use as a key for qualifiers in the output |
| keyName | the name to use for the key in the qualifier; defaults to the name of the attribute |
| prefix | a prefix to use for all metrics gathered from the current level |
| values | the attributes for which metrics are to be output |
| type | a filter for subtypes. If specified, only those objects whose 'type' attribute matches will be collected |

Note that all fields other than the above will be interpreted as collections of values.

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
webapp_config_deployment_state{app="myapp",name="aWebApp"}                                                             
webapp_config_open_sessions_high_count{app="myapp",name="aWebApp"}
weblogic_servlet_invocation_total_count{app="myapp",name="aWebApp",servletName="servlet1"}                                                             
weblogic_servlet_invocation_total_count{app="myapp",name="aWebApp",servletName="simpleServlet"}                                                             
```                                                             
Note that no help or type text is current produced, as the REST API has no access to the underlying mbean info.


## Self-Monitoring

The exporter produces metrics for monitoring its own performance:

- `wls_scrape_mbeans_count_total` reports the number of metrics scraped
- `wls_scrape_duration_seconds` reports the time required to do the scrape
- `wls_scrape_cpu_seconds` reports the CPU time used during the scrape

