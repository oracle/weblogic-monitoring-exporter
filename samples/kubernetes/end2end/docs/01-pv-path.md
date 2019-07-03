## Configuring the PV Path
In this sample, we need to create four Persistent Volumes (PV) and Persistent Volume Claims (PVC) to store data for MySQL, Prometheus server, Prometheus Alertmanager, and Grafana. See the YAML files for the four PVs/PVCs.
- [PV and PVC for MYSQL server](../mysql/persistence.yaml).
- [PV and PVC for Prometheus server](../prometheus/persistence.yaml).
- [PV and PVC for Prometheus Alertmanager](../prometheus/alert-persistent.yaml).
- [PV and PVC for Grafana server](../grafana/persistence.yaml).

> **Note**: To simply the configuration, we use `hostPath` PV that can only work in a single-node Kubernetes cluster; therefore, it's for demonstration purposes only. In a production environment, you would need to use a more sophisticated PV type like NFS, iSCSI. For more details, see the Kubernetes [Types of Persistent Volumes](https://kubernetes.io/docs/concepts/storage/persistent-volumes/#types-of-persistent-volumes) document.  

To simplify configuration and management, the four PVs will share the same root path. Prepare a host folder as the root path of the PVs.

Create a new folder in the host machine and set the `PV_ROOT` env.
```
mkdir <someDIR>
export PV_ROOT=<someDIR>
```

Then use the following commands to auto-update the path values in the PV and PVC YAML files.

```
sed -i 's@%PV_ROOT%@'"$PV_ROOT"'@' mysql/persistence.yaml
sed -i 's@%PV_ROOT%@'"$PV_ROOT"'@' prometheus/persistence.yaml
sed -i 's@%PV_ROOT%@'"$PV_ROOT"'@' prometheus/alert-persistence.yaml
sed -i 's@%PV_ROOT%@'"$PV_ROOT"'@' grafana/persistence.yaml
```

### Verification

To confirm that the path value has been updated correctly, use the `grep` command to print the PV path lines in the YAML files.

```
grep -r --include="*.yaml"  'path: ' .
```
> output (assuming the `PV_ROOT` is set to `/scratch/test`)
```
./mysql/persistence.yaml:    path: "/scratch/test/monitoring/mysql_data"
./grafana/persistence.yaml:    path: "/scratch/test/monitoring/grafana"
./prometheus/alert-persistent.yaml:    path: "/scratch/test/monitoring/alertmanager"
./prometheus/persistence.yaml:    path: "/scratch/test/monitoring/prometheus"
```
Next: [Setting up MYSQL Server](02-mysql.md)
