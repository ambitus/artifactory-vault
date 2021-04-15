#!/bin/bash

# IMPORTANT: Note that this script was created to automate the upgrade of a PostgreSQL database that is used with JFrog Artifactory.
# The upgrade process will create an upgraded copy of the original database.

set -e

# Store the names of the Docker volumes and versions that will be used for the upgrade.
# Pass in command line arguments when running this script according to the
# following environment variable definitions.
export OLD_DB=$1
export NEW_DB=$2
export OLD_VERSION=$3
export NEW_VERSION=$4

# Perform upgrade
docker run --rm --user postgres \
	-e PGUSER=artifactory -e POSTGRES_INITDB_ARGS="-U artifactory" \
	-v ${OLD_DB}:/var/lib/postgresql/${OLD_VERSION}/data \
	-v ${NEW_DB}:/var/lib/postgresql/${NEW_VERSION}/data \
	pg_upgrade:${OLD_VERSION}-to-${NEW_VERSION}
