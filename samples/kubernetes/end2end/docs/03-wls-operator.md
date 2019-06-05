## Installing the WLS Operator
We depend on [the WLS operator](https://github.com/oracle/weblogic-kubernetes-operator) to create and manage WLS domains. Check [the installation doc](https://oracle.github.io/weblogic-kubernetes-operator/userguide/managing-operators/installation/).

Here we provide the detail steps to install the WLS operator with release 2.1 as an example.

Pull the WLS operator 2.1 image.
```
docker pull oracle/weblogic-kubernetes-operator:2.1
```
Pull the WebLogic 12.2.1.3 image from Oracle Container Registry site. You need a valid user/pwd to log in to the site first. And then tag the image.
```
docker login container-registry.oracle.com
docker pull container-registry.oracle.com/middleware/weblogic:12.2.1.3
docker tag container-registry.oracle.com/middleware/weblogic:12.2.1.3 store/oracle/weblogic:12.2.1.3
```

Verify that the weblogic image has the right patch set.
```
docker run store/oracle/weblogic:12.2.1.3 sh -c '$ORACLE_HOME/OPatch/opatch lspatches'
```
> output
```
29135930;One-off
27117282;One-off
26355633;One-off
26287183;One-off
26261906;One-off
26051289;One-off

OPatch succeeded.
```
Confirm the patch set `29135930` is in the patch list.

Clone the WLS operator 2.1 repo.
```
git clone -b release/2.1 https://github.com/oracle/weblogic-kubernetes-operator.git
```
Create a namespace to run WLS operator.
```
kubectl create namespace weblogic-operator1
```
Create a service account.
```
kubectl create serviceaccount -n weblogic-operator1 sample-weblogic-operator-sa
```
Install the wls operator chart.
```
helm install weblogic-kubernetes-operator/kubernetes/charts/weblogic-operator \
  --name sample-weblogic-operator \
  --namespace weblogic-operator1 \
  --set serviceAccount=sample-weblogic-operator-sa \
  --set image=oracle/weblogic-kubernetes-operator:2.1 \
  --set "domainNamespaces={default}" \
  --wait
```

### Verification
Wait until the operator pod is running and ready.
```
kubectl -n weblogic-operator1 get pod
```
> output
```
NAME                                READY   STATUS    RESTARTS   AGE
weblogic-operator-fcfff877c-c4972   1/1     Running   0          30h
```
Wait and until the domain CRD is registered successfully.
```
kubectl get crd domains.weblogic.oracle
```
> output
```
NAME                      CREATED AT
domains.weblogic.oracle   2019-05-28T07:17:26Z
```

Now the WLS operator is running and it's monitoring the default namespace. Later we'll deploy a domain resource to default namespace and the operator is responsible for actually creating, running and managing the WLS domain.

Next: [Running a WLS Domain](04-wls-domain.md)


