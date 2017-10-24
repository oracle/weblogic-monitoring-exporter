WLS Prometheus Exporter Coordinator
=====

Each instance in a WLS cluster maintains the configuration for its 
[Prometheus Exporter](http://github.com/oracle/wls_exporter) locally. The coordinator allows them to share
changes to the query configurations.

The coordinator is a simple web server, listening on port 8999, which accepts two requests:
- a GET request returns the latest configuration update
- a PUT request provides a new configuration update

The following is an example configuration update:
```
{
    "timestamp": 14567894,
    "configuration": 
        "queries:
           - JVMRuntime:
              key: name
              prefix: jvm_
              values: [heapFreeCurrent, heapFreePercent, heapSizeCurrent, heapSizeMax, uptime, processCpuLoad]"
}
```
An update is a JSON object with two fields:

| Name | Description |
| --- | --- |
| timestamp | a long value, representing milliseconds since the epoch. |
| configuration | a string representing the new YAML configuration. |

## Building

This server is run in a docker container, which is built using

`docker build -t config_coordinator .`

from the config_coordinator directory, and may be run with
 
`docker run --publish 8999:8999 --name coordinator --rm config_coordinator`

The `query_sync` section of the exporter configuration should point to this server
