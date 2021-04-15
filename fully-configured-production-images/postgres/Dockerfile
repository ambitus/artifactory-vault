FROM icr.io/ibmz/postgres:<version>

USER postgres

# Get SSL config and SSL cert and key
WORKDIR /var/lib/postgresql/certs-conf
COPY cert.pem .
COPY key.pem .
COPY postgresql.conf postgresql.conf

# Get new entrypoint script
COPY docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh

WORKDIR /
ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["postgres"]
