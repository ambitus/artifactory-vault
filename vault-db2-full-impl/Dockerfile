FROM jfrog/artifactory-oss:6.19.1

COPY entrypoint-db2.sh /entrypoint-db2.sh
COPY DbType.java /opt/jfrog/artifactory/webapps/
COPY DbUtils.java /opt/jfrog/artifactory/webapps/

COPY update-dependencies.sh /update-dependencies.sh
RUN ./update-dependencies.sh

COPY db2jcc4.jar /opt/jfrog/artifactory/tomcat/lib/
COPY db2jcc_license_cisuz.jar /opt/jfrog/artifactory/tomcat/lib/
ENV CLASSPATH=/opt/jfrog/artifactory/tomcat/lib/db2jcc4.jar:/opt/jfrog/artifactory/tomcat/lib/db2jcc_license_cisuz.jar:/opt/jfrog/artifactory/tomcat/webapps/access/WEB-INF/lib/*:$CLASSPATH

ENTRYPOINT [ "/entrypoint-db2.sh"]
