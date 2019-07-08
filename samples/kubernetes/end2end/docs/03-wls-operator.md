## Installing the WebLogic Kubernetes Operator
We depend on the [WebLogic Kubernetes Operator](https://github.com/oracle/weblogic-kubernetes-operator) to create and manage WebLogic domains. For detailed installation information, see [Install the Operator](https://oracle.github.io/weblogic-kubernetes-operator/userguide/managing-operators/installation/).

In this example, we provide the steps to install the 2.1 release of the WebLogic Kubernetes Operator.

### Pulling the Images
We need two images, the WebLogic Kubernetes Operator image from [Docker Hub](https://hub.docker.com) and the WebLogic Server image from [Oracle Container Registry](https://container-registry.oracle.com).  
Before pulling the image, you need to:
- Aquire an user account to the image site.
- Log in to the site in your browser, find the image and accept the license.

Pull the WebLogic Kubernetes Operator 2.1 image.
```
docker login
docker pull oracle/weblogic-kubernetes-operator:2.1
```
Pull the WebLogic Server 12.2.1.3 image.
```
docker login container-registry.oracle.com
docker pull container-registry.oracle.com/middleware/weblogic:12.2.1.3
```

Verify that the WebLogic image has the right patch set.
```
docker run container-registry.oracle.com/middleware/weblogic:12.2.1.3  sh -c '$ORACLE_HOME/OPatch/opatch lspatches'
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
Confirm that patch set `29135930` is in the patch list.

### Installing the WebLogic Operator

Clone the WebLogic operator 2.1 repo.
```
git clone -b release/2.1 https://github.com/oracle/weblogic-kubernetes-operator.git
```
Create a namespace in which to run the WebLogic operator.
```
kubectl create namespace weblogic-operator1
```
Create a service account.
```
kubectl create serviceaccount -n weblogic-operator1 sample-weblogic-operator-sa
```
Install the WebLogic operator chart.
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
Wait until the domain CRD is registered successfully.
```
kubectl get crd domains.weblogic.oracle
```
> output
```
NAME                      CREATED AT
domains.weblogic.oracle   2019-05-28T07:17:26Z
```

Now the WebLogic Kubernetes Operator is running and it's monitoring the default namespace. Later we'll deploy a domain resource to the default namespace and the operator will be responsible for creating, running, and managing the WebLogic domain.

Next: [Running a WebLogic Domain](04-wls-domain.md)
