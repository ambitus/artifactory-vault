# Please fill all <placeholders> for database version arguments before building.

# Define full version number.
ARG OLD_VERSION_FULL=<FULL VERSION NUMBER>
ARG NEW_VERSION_FULL=<FULL VERSION NUMBER>

FROM icr.io/ibmz/postgres:${OLD_VERSION_FULL} AS old-bin-share
# Used to get old binaries and share folder.


FROM icr.io/ibmz/postgres:${NEW_VERSION_FULL}

# Define version arguments as integers. (ie., Use "12" instead of "12.4")
ARG OLD_VERSION=<INTEGER VERSION NUMBER ONLY>
ARG NEW_VERSION=<INTEGER VERSION NUMBER ONLY>

# Copy over old binaries and share folder.
COPY --from=old-bin-share "/usr/lib/postgresql/${OLD_VERSION}" "/usr/lib/postgresql/${OLD_VERSION}"
COPY --from=old-bin-share "/usr/share/postgresql/${OLD_VERSION}" "/usr/share/postgresql/${OLD_VERSION}"

# Specify old and new binaires for pg_upgrade.
ENV PGBINOLD "/usr/lib/postgresql/${OLD_VERSION}/bin"
ENV PGBINNEW "/usr/lib/postgresql/${NEW_VERSION}/bin"

# Specify old and new data dirs.
ENV PGDATAOLD "/var/lib/postgresql/${OLD_VERSION}/data"
ENV PGDATANEW "/var/lib/postgresql/${NEW_VERSION}/data"

# Create old and new data dirs.
RUN mkdir -p "$PGDATAOLD" "$PGDATANEW" \
	&& chown -R postgres:postgres /var/lib/postgresql

WORKDIR /var/lib/postgresql

# Get the docker upgrade script.
COPY docker-upgrade /usr/local/bin/

ENTRYPOINT ["docker-upgrade"]

# Don't use --link. We want to make a complete upgraded copy of the db.
CMD ["pg_upgrade"]
