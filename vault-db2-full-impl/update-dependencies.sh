#!/bin/bash

cd /opt/jfrog/artifactory/webapps
javac DbType.java -d tmp/WEB-INF/lib/
cd /opt/jfrog/artifactory/webapps
mv access.war tmp/
cd tmp
jar -xvf access.war
rm -rf access.war
cd WEB-INF/lib
jar -uvf jfrog-db-infra-3.12.0.jar org/jfrog/storage/DbType.class
cd ../../..
javac DbUtils.java -cp /opt/jfrog/artifactory/webapps/tmp/WEB-INF/lib/jfrog-db-infra-3.12.0.jar:/opt/jfrog/artifactory/webapps/tmp/WEB-INF/lib/lombok-1.18.2.jar:/opt/jfrog/artifactory/webapps/tmp/WEB-INF/lib/slf4j-api-1.7.26.jar:/opt/jfrog/artifactory/webapps/tmp/WEB-INF/lib/spring-jdbc-5.1.15.RELEASE.jar:/opt/jfrog/artifactory/webapps/tmp/WEB-INF/lib/javax.annotation-api-1.3.2.jar:/opt/jfrog/artifactory/webapps/tmp/WEB-INF/lib/commons-lang-2.6.jar:/opt/jfrog/artifactory/webapps/tmp/WEB-INF/lib/jsr305-2.0.0.jar -d tmp/WEB-INF/lib/
cd tmp/WEB-INF/lib
jar uvf jfrog-db-infra-3.12.0.jar org/jfrog/storage/util/DbUtils.class
jar uvf jfrog-db-infra-3.12.0.jar org/jfrog/storage/util/DbUtils\$1.class
mv org ../../../
cd ../..
jar -cvf access.war *
mv access.war ../
cd
