## Setting up a Webhook
Let's set up a simple webhook as the notification receiver. The webhook is written in [a Python script](../webhook/scripts/server.py) which simply logs all the received notifications. Typically, webhook receivers are often used to notify systems that Alertmanager doesn’t support directly.  

Build the webhook image.
```
docker build ./webhook -t webhook-log:1.0
```
Create a new namespace and install the webhook server.
```
kubectl create ns webhook
kubectl apply -f ./webhook/server.yaml
```
Wait until the webhook server pod is running and ready.
```
kubectl -n webhook get pod -l app=webhook
```
> output
```
NAME                      READY   STATUS    RESTARTS   AGE
webhook-bbfdffd48-tpzkl   1/1     Running   0          1h
```
Check the created service.
```
kubectl -n webhook get svc -l app=webhook
```
> output
```
NAME      TYPE        CLUSTER-IP      EXTERNAL-IP   PORT(S)    AGE
webhook   ClusterIP   10.102.22.170   <none>        8080/TCP   1h
```
Now the webhook server is ready.

Next: [Firing Alerts](08-alert.md)
