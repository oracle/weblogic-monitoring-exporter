## Cleanup
Follow the steps below to clean up all the resources created in this sample.

### Delete Grafana
Delete the Grafana chart.
```
helm delete --purge grafana
```

Delete the Grafana administrative credentials.
```
kubectl -n monitoring delete secret grafana-secret
```

Delete the PV and PVC used by the Grafana server.
```
kubectl delete -f grafana/persistence.yaml
```
### Delete Prometheus
Delete the Prometheus chart.
```
helm delete --purge prometheus
```

Delete the PV and PVC used by the Prometheus server.
```
kubectl delete -f prometheus/persistence.yaml
```

Now we can delete the `monitoring` namespace.
```
kubectl delete ns monitoring
```

### Delete WLS Domains
Delete the domain resource.
```
kubectl delete -f demo-domains/domain1.yaml
```

Delete the WLS administrative credentials secret.
```
kubectl delete secret domain1-weblogic-credentials
```

Wait until all the pods in the domain are deleted.
```
kubectl get pod -l weblogic.domainName=domain1
```
> Expected result:
```
No resources found.
```

Wait until all the services in the domain are deleted.
```
kubectl get service -l weblogic.domainName=domain1
```
> Expected result:
```
No resources found.
```

### Delete the WLS Kubernetes Operator
Wait until all the WLS domain related resources are deleted before deleting the operator.

Delete the operator chart.
```
helm delete --purge sample-weblogic-operator
```

Delete the service account.
```
kubectl delete -n weblogic-operator1 serviceaccount sample-weblogic-operator-sa
```

Delete the namespace.
```
kubectl delete namespace weblogic-operator1
```

Delete the folder containing the operator repository.
```
rm -rf weblogic-kubernetes-operator
```

### Delete MYSQL Server
Delete the MYSQL server.
```
kubectl delete  -f ./mysql/mysql.yaml
```

Delete the PV and PVC used by the MYSQL server.
```
kubectl delete  -f ./mysql/persistence.yaml
```

### Delete the PV from the Host Folder
To get around file permission checking under this folder, we do the deletion in a Docker container with root privilege.
```
docker run --rm -v $PV_ROOT:/tt -v $PWD/util:/util  nginx /util/clean-pv.sh
```
