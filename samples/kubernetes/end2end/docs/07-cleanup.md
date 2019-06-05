## Cleanup
Follow the steps below to clean up all the resources created in this sample.

### Delete Grafana
Delete the Grafana chart.
```
helm delete --purge grafana
```

Delete Grafana admin credential.
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

Delete the secrete of WLS admin credential.
```
kubectl delete secret domain1-weblogic-credentials
```

Wait until all the pods the domain are deleted.
```
kubectl get pod -l weblogic.domainName=domain1
```
> Expected result:
```
No resources found.
```

Wait until all the services the domain are deleted. 
```
kubectl get service -l weblogic.domainName=domain1
```
> Expected result:
```
No resources found.
```

### Delete the WLS Operator
Please wait until all WLS domain related resources are deleted before go ahead to delete the WLS operator.

Delete the wls operator chart.
```
helm delete --purge sample-weblogic-operator
```

Delete the service account
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
Delete the mysql server.
```
kubectl delete  -f ./mysql/mysql.yaml
```

Delete the PV and PVC used by the mysql server.
```
kubectl delete  -f ./mysql/persistence.yaml
```

### Clean the Host Folder of PV
To get around the permission checking of files under this folder, we do the deletion in a docker container with root privilege.
```
docker run --rm -v $PV_ROOT:/tt -v $PWD/util:/util  nginx /util/clean-pv.sh
```
