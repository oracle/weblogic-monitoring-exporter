#!/bin/bash
# This scripts is supposed to be run in a docker container to build the webapp war file.

cd /tt
jar cf testwebapp.war index.jsp WEB-INF/weblogic.xml WEB-INF/web.xml
chmod 666 testwebapp.war
