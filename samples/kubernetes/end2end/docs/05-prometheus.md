## Setting up Prometheus
Install Prometheus using a [Helm chart](https://github.com/helm/charts/tree/master/stable/prometheus) with the customized [values.yaml](../prometheus/values.yaml). There are four components will be installed with this chart: Prometheus server, Alertmanager, node-exporter and kube-state-metrics.  

Create a new namespace `monitoring`.
```
kubectl create ns monitoring
```
Deploy the PV and PVC YAML files.
```
kubectl apply -f prometheus/persistence.yaml
kubectl apply -f prometheus/alert-persistence.yaml
```
Install the Prometheus chart.
```
helm install --wait --name prometheus --namespace monitoring --values  prometheus/values.yaml stable/prometheus
```
Note that in the customized values.yaml file we add a new job [wls-domain1](../prometheus/values.yaml#L56) to scrape data from the WLS domain created in the previous step.

### Verification
Wait until all the Prometheus pods are running and ready.
```
kubectl -n monitoring get pod -l app=prometheus
```
> output
```
NAME                                            READY   STATUS    RESTARTS   AGE
prometheus-alertmanager-7456cdb5b8-9vfmd        2/2     Running   0          1h
prometheus-kube-state-metrics-dbfff5557-f22xx   1/1     Running   0          1h
prometheus-node-exporter-6n7rj                  1/1     Running   0          1h
prometheus-server-5789fffc86-mmh8f              2/2     Running   0          1h
```
Check the Prometheus sevices.
```
kubectl -n monitoring get svc -l app=prometheus
```
> output
```
NAME                            TYPE        CLUSTER-IP       EXTERNAL-IP   PORT(S)        AGE
prometheus-alertmanager         NodePort    10.108.31.234    <none>        80:32000/TCP   1h
prometheus-kube-state-metrics   ClusterIP   None             <none>        80/TCP         1h
prometheus-node-exporter        ClusterIP   None             <none>        9100/TCP       1h
prometheus-server               NodePort    10.103.186.222   <none>        80:30000/TCP   1h
```

### Access the Prometheus web UI
Now you can access the Prometheus web UI in your browser with the URL `http://<HostIP>:30000`.  
In the top menu, click `Status` and then `Targets`. The target page is displayed. Go to the bottom of the page. You'll find that the two endpoints of `wls-domain1` are up and healthy.

![Prometheus Targets](./images/prometheus-targets.png)

We'll talk about Alertmanager in later step.

Next: [Setting up Grafana](06-grafana.md)
