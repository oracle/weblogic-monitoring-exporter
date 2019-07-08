## Setting up MYSQL Server

In this step, we describe how to set up MySQL database in the Kubernetes cluster. In this sample, we are using the MySQL database to store application data. The WebLogic domain created in a later step will store its data to the database, for example, persistent JMS messages.  

Deploy the PV and PVC.
```
kubectl apply -f ./mysql/persistence.yaml
```
Install a MySQL server.   

Note that the root password is set to `123456` in the [mysql yaml](../mysql/mysql.yaml). Change it to a more secure password when needed.
```
kubectl apply -f ./mysql/mysql.yaml
```
Wait until the MySQL server pod is running and ready.
```
kubectl get pod -l app=mysql
```
> output
```
NAME                    READY   STATUS    RESTARTS   AGE
mysql-7bf4d88f9-sbd25   1/1     Running   0          31h
```
Get the pod name of MySQL server.
```
POD_NAME=$(kubectl get pod -l app=mysql -o jsonpath="{.items[0].metadata.name}")
```
Create a new database with name `domain1` and a new user `wluser1` with password `wlpwd123`.  
```
kubectl exec -it $POD_NAME -- mysql -p123456 -e "CREATE DATABASE domain1;"
kubectl exec -it $POD_NAME -- mysql -p123456 -e "CREATE USER 'wluser1' IDENTIFIED BY 'wlpwd123';"
kubectl exec -it $POD_NAME -- mysql -p123456 -e "GRANT ALL ON domain1.* TO 'wluser1';"
```

## Verification
Access the new database with the new user.
```
kubectl exec -it $POD_NAME -- mysql -u wluser1 -pwlpwd123 -D domain1 -e "show tables;"
```
Note that we have not created any user tables in the database. The purpose of running this query is to verify that the MySQL database has been created correctly.

Next: [Installing the WebLogic Kubernetes Operator](03-wls-operator.md)
