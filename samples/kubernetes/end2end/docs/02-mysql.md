## Setting up MYSQL Server
Deploy PV and PVC.
```
kubectl apply -f ./mysql/persistence.yaml
```
Install a mysql server. Note that the root password is set to `123456` in [the mysql yaml](../mysql/mysql.yaml).
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
Create a new database and a new user that will be used by WebLogic domain later.
```
POD_NAME=$(kubectl get pod -l app=mysql -o jsonpath="{.items[0].metadata.name}")
kubectl exec -it $POD_NAME -- mysql -p123456 < ./mysql/setup.sql
```

## Verification
Check that the database `domain1` is created.
```
kubectl exec -it $POD_NAME -- mysql -p123456 -e 'show databases;'
```
Confirm that the database `domain1` is in the output list.  
> output
```
+--------------------+
| Database           |
+--------------------+
| information_schema |
| domain1            |
| mysql              |
| performance_schema |
+--------------------+
```

Check that new user `weblogic` is created.
```
kubectl exec -it $POD_NAME -- mysql -p123456 -e 'SELECT User FROM mysql.user;'
```
Confirm that the user `weblogic` is in the output list.  
> output
```
+----------+
| User     |
+----------+
| root     |
| weblogic |
| root     |
+----------+
```

Now the mysql database is ready. The WLS domain created in later step will store its data to this database.  

Next: [Installing the WLS Operator](03-wls-operator.md)
