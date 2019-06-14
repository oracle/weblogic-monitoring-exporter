## What's in the WebLogic Server Dashboard
### Overview
The WebLogic Server Grafana dashboard provides a visualization of WebLogic Server runtime metrics allowing you to monitor and diagnose the runtime deployment of WebLogic Server.

At the top, left-hand corner of the dashboard page, there are filters which let you filter out metrics based on different domains, different clusters, and different servers. Another filter is the `Top N` whose candidate values are 3, 5, 7, and 10. The value will be applied to panels to show only the top N elements, thus avoiding too many elements being displayed.  

The WebLogic Server dashboard has four rows: `Servers`, `Web Applications`, `Data Sources` and `JMS Services`. Each row can be folded and unfolded separately.  

![Dashboard Overview](./images/dashboard-overview.png)

### Servers
![Servers](./images/dashboard-servers.png)

### Web Applications
![Web Applications](./images/dashboard-webapp.png)

### Data Sources
![Data Sources](./images/dashboard-datasources.png)

### JMS Services
![JMS Services](./images/dashboard-jms.png)
