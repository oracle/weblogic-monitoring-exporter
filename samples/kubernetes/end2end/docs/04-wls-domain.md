## Running a WLS Domain
The WLS operator has documents about creating domains, e.g. different considerations and different options, see detail info in [managing domains guide](https://oracle.github.io/weblogic-kubernetes-operator/userguide/managing-domains/). The only extra step needed to integrate with Prometheus is to install the wls-exporter web application to WLS servers/clusters.

In this task, we provide concrete scripts to create a demonstration domain.

### Set Proxy Before Run
In the scripts to build the domain image, we use `wget` to donwload WebLogic Deploy Tool archive from GitHub. If you run the script behind a proxy, you need to configure the proxy setting properly first.  
One of the approaches is to use proxy variables:
```
export http_proxy=http://proxy_host:proxy_port
export https_proxy=$http_proxy
```
Verify the download with `wget` can work correctly.
```
wget https://github.com/oracle/weblogic-deploy-tooling/releases/download/weblogic-deploy-tooling-0.24/weblogic-deploy.zip \
-O /dev/null
```

### Build the Domain Image
A script wrapper [build.sh](../demo-domains/domainBuilder/build.sh) is used to do this. See the script usage below:
```
usage: ./build.sh domainName adminUser adminPwd mysqlUser mysqlPwd
```
Build a domain image.
```
cd demo-domains/domainBuilder
./build.sh domain1 weblogic welcome1  wluser1 wlpwd123
cd ../..
```
A docker image `domain1-image:1.0` will be created. `weblogic` and `welcome1` are the domain's admin user name and password. `wluser1` and  `wlpwd123` are username and password of mysql database. The database name is the same as the domain name so the database used by `domain1` is `domain1`.

The domain configuration is burned into the image. What's in the domain configuration?
- One data source, one JDBC store, one JMS server and a JMS module. All the resources are deployed to the cluster. 
- Two web applications deployed to cluster: test-webapp and wls-exporter.  

Check the exporter configuration [exporter-config.yaml](../dashboard/exporter-config.yaml).

### Deploy the Domain Resource
Create a secret for WLS admin credential.
```
kubectl -n default create secret generic domain1-weblogic-credentials \
      --from-literal=username=weblogic \
      --from-literal=password=welcome1
```
Deploy the domain resource. The resource is deployed to `default` namespace.
```
kubectl apply -f demo-domains/domain1.yaml
```
Now the WLS operator is supposed to detect this new domain resource and actually run a WebLogic domain based on it.

### Verification
Wait until the three wls server pods are running and ready.
```
kubectl get pod -l weblogic.domainName=domain1
```
> output
```
NAME                       READY   STATUS    RESTARTS   AGE
domain1-admin-server       1/1     Running   0          32h
domain1-managed-server-1   1/1     Running   0          32h
domain1-managed-server-2   1/1     Running   0          32h
```
Check the generated services.
```
kubectl get service -l weblogic.domainName=domain1
```
> output
```
NAME                            TYPE        CLUSTER-IP       EXTERNAL-IP   PORT(S)                          AGE
domain1-admin-server            ClusterIP   None             <none>        30012/TCP,7001/TCP               32h
domain1-admin-server-external   NodePort    10.109.88.166    <none>        30012:30703/TCP,7001:30701/TCP   32h
domain1-cluster-cluster-1       ClusterIP   10.107.190.137   <none>        8001/TCP                         32h
domain1-managed-server-1        ClusterIP   None             <none>        8001/TCP                         32h
domain1-managed-server-2        ClusterIP   None             <none>        8001/TCP                         32h
```

You can access the WLS admin console from your browser via URL `http://<hostname>:30701/console`.

### Check the WLS Runtime Metrics
The exported metrics are plain text and human-readable. We can use `curl` or similar tools to check the metrics.

Let's deploy curl tool.
```
kubectl apply -f ./util/curl.yaml
```
Wait until the curl pod is running.
```
kubectl get pod curl
```
> output
```
NAME   READY   STATUS    RESTARTS   AGE
curl   1/1     Running   5          1h
```
Access metrics of `managed-server-1`.
```
kubectl exec curl -- curl http://weblogic:welcome1@domain1-managed-server-1:8001/wls-exporter/metrics
```
Access metrics of `managed-server-2`.
```
kubectl exec curl -- curl http://weblogic:welcome1@domain1-managed-server-2:8001/wls-exporter/metrics
```
> output
```
wls_server_admin_server_listen_port{location="managed-server-1"} 7001
wls_server_open_sockets_current_count{location="managed-server-1"} 8
wls_server_state_val{location="managed-server-1"} 2
wls_servlet_execution_time_average{location="managed-server-1",app="bea_wls_cluster_internal",name="managed-server-1_/bea_wls_cluster_internal",servletName="FileServlet"} 0
...
wls_webapp_config_deployment_state{location="managed-server-1",app="bea_wls_cluster_internal",name="managed-server-1_/bea_wls_cluster_internal"} 2
...
wls_datasource_active_connections_average_count{name="Generic1"} 2
...
wls_jms_bytes_current_count{jmsruntime="managed-server-1.jms",jmsserver="JMSServer1@managed-server-1"} 360324
wls_jms_bytes_high_count{jmsruntime="managed-server-1.jms",jmsserver="JMSServer1@managed-server-1"} 360324
...
```
All the metric names are started with "wls_" and metric names of different comonents have different prefix which match to what we configured in [the exporter configuration](../dashboard/exporter-config.yaml). 

Next: [Installing Prometheus](05-prometheus.md)
