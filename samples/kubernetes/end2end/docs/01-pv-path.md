## Configuring PV Path
In this sample we need to create three PersistentVolumes (PV) and PersistentVolumeClaims(PVC) to store data for MySQL, Prometheus, and Grafana. See the yaml files for the three PVs/PVCs.
- [PV and PVC for MYSQL server](../mysql/persistence.yaml).
- [PV and PVC for Prometheus server](../prometheus/persistence.yaml).
- [PV and PVC for Grafana server](../grafana/persistence.yaml).

> **Note**: To simply the configuration, we use `hostPath` PV that can only work in a single-node k8s cluster so it's only for demonstration purpose. In production environment you need to change to use more sophisticated PV type like NFS, iSCSI. See detail in [k8s PV doc](https://kubernetes.io/docs/concepts/storage/persistent-volumes/#types-of-persistent-volumes).  

To simplify the configuration and management the three PVs will share the same root path. Prepare a host folder as the root path of the PVs.

Create a new folder in the host machine and set `PV_ROOT` env.
```
mkdir <someDIR>
export PV_ROOT=<someDIR>
```

Then use the following commands to auto update the path values in the pv&pvc yamls.
```
sed -i 's@%PV_ROOT%@'"$PV_ROOT"'@' mysql/persistence.yaml
sed -i 's@%PV_ROOT%@'"$PV_ROOT"'@' prometheus/persistence.yaml
sed -i 's@%PV_ROOT%@'"$PV_ROOT"'@' grafana/persistence.yaml
```

### Verification
To confirm that the path value has been updated correctly, use command `grep` to print the PV path lines in the yaml files.
```
grep -r --include="*.yaml"  'path: ' .
```
> output (assuming the `PV_ROOT` is set to `/scratch/test`)
```
mysql/persistence.yaml:    path: "/scratch/test/monitoring/mysql_data"
grafana/persistence.yaml:    path: "/scratch/test/monitoring/grafana"
prometheus/persistence.yaml:    path: "/scratch/test/monitoring/prometheus"
```
Next: [Setting up MYSQL Server](02-mysql.md)
