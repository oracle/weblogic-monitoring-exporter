REST Exporter (experimental)
=====

WLS REST to Prometheus exporter.

A Collector that uses the [WLS proprietary REST API](https://docs.oracle.com/middleware/1221/wls/WLRUR/overview.htm#WLRUR111) to scrape runtime information. It is intended to be deployed as a
web application in a WLS instance. Typically, this is deployed to the instance you want to scrape.

## Creating a webapp

To deploy, you will need to create a web app. A [sample maven project](samples) is included. 

## Building

`mvn package` to build.

## Configuration
Here is an example `yaml` configuration:
```
host: localhost
port: 7001
username: myuser
password: mysecretpassword
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
Note that there are two parts to the configuration. The top portion tells the exporter how to connect to the REST API;
the fields are required, and their purpose should be obvious.

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
webapp_config_deploymentState{app="myapp",name="aWebApp"}                                                             
webapp_config_openSessionsHighCount{app="myapp",name="aWebApp"}
weblogic_servlet_invocationTotalCount{app="myapp",name="aWebApp",servletName="servlet1"}                                                             
weblogic_servlet_invocationTotalCount{app="myapp",name="aWebApp",servletName="simpleServlet"}                                                             
```                                                             
Note that no help or type text is current produced, as the REST API has no access to the underlying mbean info.
