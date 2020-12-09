WebLogic Monitoring Exporter Coordinator
=====

Each instance in a WLS cluster maintains the configuration locally for its
WebLogic Monitoring Exporter. The coordinator allows them to share
changes to the query configurations.

The coordinator is a simple web server, listening on port 8999, which accepts two requests:
- A GET request returns the latest configuration update
- A PUT request provides a new configuration update

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
| timestamp | A long value, representing milliseconds since the epoch. |
| configuration | A string representing the new YAML configuration. |

## Building

This server is run in a Docker container, which is built using:

`docker build -t config_coordinator .`

from the config_coordinator directory, and may be run with:

`docker run --publish 8999:8999 --name coordinator --rm config_coordinator`

The `query_sync` section of the exporter configuration should point to this server

## Persistence

Passing the option `-db <file path>` will cause the coordinator to use the designated file path to persist changes
to the configuration. This will preserve changes across container restarts. When running in a container, the file
must be in a mounted volume.

For example:

```
docker run --publish 8999:8999 --name coordinator --rm -v /my/storage:/var/lib/coordinator \
            config_coordinator -db /var/lib/coordinator/configs.json
```

 This will cause Docker to mount the host machine directory `/my/storage` at the path `/var/lib/coordinator`
 in the container, and to use a file named `configs.json` in that directory to persist its state.

 ## Copyright

 Copyright (c) 2017, 2019, Oracle Corporation and/or its affiliates. All rights reserved.
