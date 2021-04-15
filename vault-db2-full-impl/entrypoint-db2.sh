#!/bin/bash

cd /opt/jfrog/artifactory/misc/db

echo 'type=db2
url=jdbc:db2://aqmvsoe.pok.ibm.com:6002/M05DB22
driver=com.ibm.db2.jcc.DB2Driver
username=username
password=password' > db2.properties

/entrypoint-artifactory.sh
