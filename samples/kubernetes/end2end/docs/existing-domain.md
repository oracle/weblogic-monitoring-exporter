## Monitoring an Existing WebLogic Domain
In this step, we show you how to integrate Prometheus and Grafana with an existing WebLogic domain.

We assume that you have already followed the [Manage WebLogic Domains](https://oracle.github.io/weblogic-kubernetes-operator/userguide/managing-domains/) guide in the WebLogic Server Kubernetes Operator to run a WebLogic domain and have followed the previous steps in this sample to setup Prometheus and Grafana.

### Installing the `wls-exporter` to the WebLogic domain
You need to install the `wls-exporter` web application to the WebLogic domain.

First, get the `wls-exporter` WAR file following the [Downloading the release](https://github.com/oracle/weblogic-monitoring-exporter#downloading-the-release) instructions using the configuration file in this sample [exporter-config.yaml](../dashboard/exporter-config.yaml) file. Then deploy the WAR to the WebLogic Servers and clusters using WLST or WDT.  

After the `wls-exporter` is installed successfully, you can check the exported WebLogic runtime metrics from each server. See the detailed instructions [here](04-wls-domain.md#check-the-weblogic-runtime-metrics).

### Updating the Prometheus Configuration
Next, we need to instruc the Prometheus server to scrape metrics from the WebLogic domain. Basically, we need to add a new job to the `prometheus.yml` file, the configuration file of the Prometheus server. The job is similar to what we added for `domain1` in [values.yaml](../prometheus/values.yaml#L59) of the Prometheus chart. As shown below, lines in red need to be updated properly.

![job](images/prometheus-job.png)

Because the Prometheus server is already running, can we update its configuration dynamically without restarting it? The answer is yes. When installing with the Prometheus chart, there are two containers running in the Prometheus server pod: one is for the Prometheus server, the other is to detect changes in the ConfigMap and apply the latest configuration to the runtime. What we need to do is to update the ConfigMap of the Prometheus server to add a new job to the `prometheus.yml` file.

You can edit the ConfigMap directly using `kubectl edit`.
```
kubectl -n monitoring edit cm prometheus-server
```
Or you can save, update, and re-apply the ConfigMap.
1. Save the current ConfigMap to a temporary file `cm.yaml`.  
   `kubectl -n monitoring get cm prometheus-server -oyaml > cm.yaml`
1. Update the file `cm.yaml` accordingly.  
   `vi cm.yaml`
1. Apply the new ConfigMap.  
   `kubectl -n monitoring apply -f cm.yaml`

Wait until the new configuration is applied to the runtime. You can access the Prometheus web UI to verify that new targets are added and new metrics are scraped by the Prometheus server. Follow the instructions [here](05-prometheus.md#access-the-prometheus-web-ui).

### Integrating with Grafana and Dashboard
You do not need to change anything in the Grafana configuration or the WebLogic dashboard. After the Prometheus server starts to scrape and store metrics of the new WebLogic domain, the metrics will be displayed in the WebLogic dashboard seamlessly. When you access the WebLogic dashboard in the Grafana web UI, the new domain name will be displayed in the drop-down list of the `domain` filter.
