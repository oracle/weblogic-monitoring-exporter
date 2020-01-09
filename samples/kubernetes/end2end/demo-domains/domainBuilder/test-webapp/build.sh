#!/bin/bash

cd /tt
jar cf testwebapp.war index.jsp WEB-INF/weblogic.xml WEB-INF/web.xml
chmod 666 testwebapp.war
