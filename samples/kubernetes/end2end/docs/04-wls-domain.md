## Running a WLS Domain
The WLS operator has documents about creating domains, e.g. different considerations and different options, see detail info in [managing domains guide](https://oracle.github.io/weblogic-kubernetes-operator/userguide/managing-domains/). The only extra step needed to integrate with Prometheus is to install the wls-exporter web application to WLS servers/clusters.

In this task, we provide concrete scripts to create a demonstration domain.

Build the domain image with WDT.
```
cd demo-domains/domainBuilder
./build.sh domain1 weblogic welcome1
cd ../..
```
A docker image `domain1-image:1.0` will be created. `weblogic` and `welcome1` are the domain's admin user name and password. The domain configuration is burned into the image. 

What's in the domain configuration?
- One data source, one JDBC store, one JMS server and a JMS module. All the resources are deployed to the cluster. 
- Two web applications deployed to cluster: test-webapp and wls-exporter.  

Check the exporter configuration [exporter-config.yaml](../dashboard/exporter-config.yaml).

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
wls_servlet_execution_time_average{location="managed-server-1",app="bea_wls_cluster_internal",name="managed-server-1_/bea_wls_cluster_internal",servletName="GroupMessageHandlerServlet"} 0
wls_servlet_execution_time_average{location="managed-server-1",app="bea_wls_cluster_internal",name="managed-server-1_/bea_wls_cluster_internal",servletName="JspServlet"} 0
......
```
All the metrics names are started with "wls_" and we use different prefix to different comonents which match to what we configured in [the exporter configuration](../dashboard/exporter-config.yaml). 

Next: [Installing Prometheus](05-prometheus.md)
