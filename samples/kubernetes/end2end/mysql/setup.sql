CREATE DATABASE domain1;

CREATE USER 'weblogic' IDENTIFIED BY 'welcome1';
GRANT ALL ON domain1.* TO 'weblogic';
