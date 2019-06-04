## Setting up MYSQL Server
Deploy PV and PVC.
```
kubectl apply -f ./mysql/persistence.yaml
```
Install a mysql server.   
Note that the root password is set to `123456` in the [mysql yaml](../mysql/mysql.yaml). Pls change to a desirable password if needed.
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
Create a new database and a new user to be used by WebLogic domain.  
You will be prompted to fill in the MySQL root password for each command below.
```
kubectl exec -it $POD_NAME -- mysql -p -e "CREATE DATABASE domain1;"
kubectl exec -it $POD_NAME -- mysql -p -e "CREATE USER 'wluser1' IDENTIFIED BY 'wlpwd123';"
kubectl exec -it $POD_NAME -- mysql -p -e "GRANT ALL ON domain1.* TO 'wluser1';"
```

## Verification
Use the new user to access the new database.
```
kubectl exec -it $POD_NAME -- mysql -u wluser1 -p -D domain1 -e "show tables;"
```
There is not table created in the database yet. Just make sure no error occur.

Now the mysql database is ready. The WLS domain created in later step will store its data to this database.  

Next: [Installing the WLS Operator](03-wls-operator.md)
