Monitoring with Prometheus and Grafana using the WebLogic Monitoring Exporter
=====

Here are some examples of deployments to start Prometheus and Grafana in the Kubernetes cluster, and an example of the RBAC policy to set up all the required permissions to access pods.

All the examples are configured to be deployed in the `monitoring` namespace. The WebLogic Server Kubernetes Operator is deployed in the `weblogic-operator` namespace and the WebLogic domain runs in the `weblogic-domain` namespace.

You can customize the examples to reflect a specific configuration.

For more details on how to set up and run Prometheus and Grafana with the WebLogic Monitoring Exporter, see the blog, [Using Prometheus and Grafana to Monitor WebLogic Server on Kubernetes](https://blogs.oracle.com/weblogicserver/use-prometheus-and-grafana-to-monitor-weblogic-server-on-kubernetes).


## Copyright

 Copyright (c) 2017, 2019, Oracle Corporation and/or its affiliates. All rights reserved.
