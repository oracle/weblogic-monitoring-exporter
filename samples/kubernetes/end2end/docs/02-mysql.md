## Setting up MYSQL Server
In this step we'll describe how to setup MySQL database in the Kubernetes cluster. In this sample we are using the MySQL database to store application data. The WLS domain created in a later step will store its data to the database, e.g. persistent JMS messages.  

Deploy PV and PVC.
```
kubectl apply -f ./mysql/persistence.yaml
```
Install a mysql server.   
Note that the root password is set to `123456` in the [mysql yaml](../mysql/mysql.yaml). Change to a more sucure password when needed.
```
kubectl apply -f ./mysql/mysql.yaml
```
Wait until the mysql server pod is running and ready.
```
kubectl get pod -l app=mysql
```
> output
```
NAME                    READY   STATUS    RESTARTS   AGE
mysql-7bf4d88f9-sbd25   1/1     Running   0          31h
```
Get pod name of mysql server.
```
POD_NAME=$(kubectl get pod -l app=mysql -o jsonpath="{.items[0].metadata.name}")
```
Create a new database and a new user.  
You will be prompted to fill in the mysql root password for each command below.
```
kubectl exec -it $POD_NAME -- mysql -p -e "CREATE DATABASE domain1;"
kubectl exec -it $POD_NAME -- mysql -p -e "CREATE USER 'wluser1' IDENTIFIED BY 'wlpwd123';"
kubectl exec -it $POD_NAME -- mysql -p -e "GRANT ALL ON domain1.* TO 'wluser1';"
```

## Verification
Access the new database with the new user.
```
kubectl exec -it $POD_NAME -- mysql -u wluser1 -p -D domain1 -e "show tables;"
```
Note that we have not created any user tables in the database and the purpose of running this query is to verify that the MySQL database has been created correctly.

Next: [Installing the WLS Operator](03-wls-operator.md)
