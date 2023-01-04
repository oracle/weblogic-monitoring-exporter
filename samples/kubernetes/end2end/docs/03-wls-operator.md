## Installing the WebLogic Server Kubernetes Operator
We depend on the [WebLogic Server Kubernetes Operator](https://github.com/oracle/weblogic-kubernetes-operator) to create and manage WebLogic domains. For detailed installation information, see [Install the Operator](https://oracle.github.io/weblogic-kubernetes-operator/managing-operators/installation/).

In this example, we provide the steps to install the operator 4.0.1 release.

### Installing the WebLogic Server Kubernetes Operator

Create a namespace in which to run the operator.
```
kubectl create namespace weblogic-operator1
```
Create a service account.
```
kubectl create serviceaccount -n weblogic-operator1 sample-weblogic-operator-sa
```
Add the operator chart repository to Helm.
```
helm repo add weblogic-operator https://oracle.github.io/weblogic-kubernetes-operator/charts
```
Install the operator with the operator chart with version `4.0.1`.
```
helm install weblogic-operator/weblogic-operator \
  --version 4.0.1 \
  --name weblogic-operator \
  --namespace weblogic-operator1 \
  --set serviceAccount=sample-weblogic-operator-sa \
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
Now the WebLogic Server Kubernetes Operator is running and it's monitoring all namespaces created  with the label `weblogic weblogic-operator=enabled`. The operator will be responsible for creating, running, and managing any WebLogic domains deployed to namespaces with such labels.

Next: [Running a WebLogic Domain](04-wls-domain.md)
