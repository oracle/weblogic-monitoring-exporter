## Installing Grafana
Install Grafana with [helm chart](https://github.com/helm/charts/tree/master/stable/grafana) and with the customized [values.yaml](../grafana/values.yaml).

Deploy PV and PVC yaml.
```
kubectl apply -f grafana/persistence.yaml
```
Create grafana admin credential.
```
kubectl --namespace monitoring create secret generic grafana-secret --from-literal=username=admin --from-literal=password=12345678
```
Install Grafana chart.
```
helm install --wait --name grafana --namespace monitoring --values grafana/values.yaml stable/grafana
```
### Verification
Wait until the Grafana pod is running.
```
kubectl -n monitoring get pod -l app=grafana
```
> output
```
NAME                       READY   STATUS    RESTARTS   AGE
grafana-7bc95b7545-dz8dg   1/1     Running   0          26h
```
Check the Grafana sevices.
```
kubectl -n monitoring get svc -l app=grafana
```
> output
```
NAME      TYPE       CLUSTER-IP     EXTERNAL-IP   PORT(S)        AGE
grafana   NodePort   10.105.155.5   <none>        80:31000/TCP   26h
```
Now you can access the Grafana web UI in your browser with URL `http://<HostIP:31000>` with user&pwd `admin:12345678`.

### Creating Data Source and Dashboard
#### via Grafana web UI
Refer to Grafana docs:
- [Creating a Prometheus Data Source](https://grafana.com/docs/features/datasources/prometheus/)
- [Importing Dashboards](https://grafana.com/docs/reference/export_import/)  

To create a Prometheus data source: 
- From the Grafana menu, choose `Configuration` -> `Data Sources` and click on `Add Data Source` button. 
- In the `Choose data source type` page click `Prometheus` button
- Use the following values to add the Prometheus data source:
  ```
  Name: Prometheus
  Default: true
  URL: http://prometheus-server:80
  ```
- Click on `Save & Test` button.
Now the Prometheus data source is created.  

To import the WebLogic dashboard:
- From the Grafana menu, choose `Dashboards` -> `Manage` and click on `Import` button. 
- In the `Import` page click `Upload .json File` button then choose the [the WebLogic dashboard file](../dashboard/weblogic_dashboard.json) to import.

#### via Grafana REST API
Alternatively, you can do this via calling Grafana REST API.  
Create the Prometheus data source with the predefined json file.
```
curl -v -H 'Content-Type: application/json' -H "Content-Type: application/json" \
  -X POST http://admin:12345678@$HOSTNAME:31000/api/datasources/ \
  --data-binary @grafana/datasource.json
```
Create the WebLogic dashboard with the predefined json file. 
```
curl -v -H 'Content-Type: application/json' -H "Content-Type: application/json" \
  -X POST http://admin:12345678@$HOSTNAME:31000/api/dashboards/db \
  --data-binary @grafana/dashboard.json
```
### Verification
Go to the Grafana web UI to confirm the resources are created successfully.

Check the datasource.  
![datasource](images/datasource.png)  

Check the WebLogic dashboard.  
![dashboard](images/weblogicDashboard.png) 

You can monitor the WebLogic domain with the dashboard now.
