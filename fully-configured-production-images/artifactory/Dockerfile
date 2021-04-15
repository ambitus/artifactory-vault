FROM icr.io/ibmz/jfrog-artifactory-oss:<version>

USER root

RUN apt-get install vim -y

USER artifactory

# Insert CA cert
WORKDIR /home/artifactory
RUN mkdir .postgresql
COPY root.crt .postgresql/

# Insert SSL certs
RUN mkdir .ssl
COPY cert.pem .ssl/
COPY key.pem .ssl/

# Add SSL config
COPY server.xml /opt/jfrog/artifactory/tomcat/conf/server.xml

# Use this port for HTTPS/SSL
EXPOSE 8443

WORKDIR /
ENTRYPOINT ["/entrypoint-artifactory.sh"]
