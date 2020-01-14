#!/bin/bash
# Copyright 2020, Oracle Corporation and/or its affiliates.  All rights reserved.
# Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
# This script is supposed to be run in a docker container to build the webapp war file.

cd /tt
jar cf testwebapp.war index.jsp WEB-INF/weblogic.xml WEB-INF/web.xml
chmod 666 testwebapp.war
