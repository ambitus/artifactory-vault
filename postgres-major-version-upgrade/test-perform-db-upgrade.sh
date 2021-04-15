#!/bin/bash

# Use this script to test your pg_upgrade image and script, to ensure it is working properly.
# Please fill all placeholders with the desired values if you have not already before running this script.

set -e

export OLD_DB_VERSION="<INTEGER VERSION NUMBER>"
export OLD_DB_VERSION_FULL="<FULL VERSION NUMBER>"
export NEW_DB_VERSION="<INTEGER VERSION NUMBER>"
export NEW_DB_VERSION_FULL="<FULL VERSION NUMBER>"
export JFROG_VERSION="<FULL VERSION NUMBER>"

export ZCX_HOST="<HOST/IP OF ZCX APPLIANCE WHERE TEST IS BEING PERFORMED>"
export ZCX_PORT_DB="<ZCX HOST PORT THAT DB WILL LISTEN ON>"
export ZCX_PORT_ARTIFACTORY="<ZCX HOST PORT THAT ARTIFACTORY WILL LISTEN ON>"

export JFROG_VOL_CONTAINER="pg-upgrade-test-artifactory"
export OLD_DB_VOL_CONTAINER="pg-upgrade-test-postgres-${OLD_DB_VERSION}"
export NEW_DB_VOL_CONTAINER="pg-upgrade-test-postgres-${NEW_DB_VERSION}"

export ANSI_YELLOW_BOLD="\e[1;33m"
export ANSI_GREEN="\e[32m"
export ANSI_RED_BACKGROUND="\e[1;7;31m"
export ANSI_YELLOW_BACKGROUND="\e[1;7;33m"
export ANSI_GREEN_BACKGROUND="\e[1;7;32m"
export ANSI_CYAN_BACKGROUND="\e[1;7;36m"
export ANSI_CYAN="\e[36m"
export ANSI_RESET="\e[0m"
export LOGS_TOP="******************************************* LOGS *********************************************"
export LOGS_BOTTOM="**********************************************************************************************"
export TEST_START="************************************* TEST PG_UPGRADE ****************************************"
export TEST_SUCCESS="************************************** TEST SUCCESSFUL ***************************************"
export TEST_FAILURE="**************************************** TEST FAILED *****************************************"


test_start () { 
	echo -e "\n${ANSI_YELLOW_BACKGROUND}${TEST_START}${ANSI_RESET}\n" 
}


test_success () { 
	echo -e "\n${ANSI_GREEN_BACKGROUND}${TEST_SUCCESS}${ANSI_RESET}\n" 
}


test_failure () {
	echo -e "\n${ANSI_RED_BACKGROUND}${TEST_FAILURE}${ANSI_RESET}\n"
}


print_status () { 
	export MESSAGE=$1

	echo -e "\n${ANSI_YELLOW_BOLD}${MESSAGE}${ANSI_RESET}\n" 
}	


print_logs () {
	export CONTAINER_NAME=$1

	echo -e "\n${ANSI_CYAN}${LOGS_TOP}${ANSI_RESET}\n\n"
	echo -e "$(docker logs ${CONTAINER_NAME})\n\n"
	echo -e "${ANSI_CYAN}${LOGS_BOTTOM}${ANSI_RESET}\n"
}


wait_for_container () {
	export SECONDS=$1
	export SLEEP_INTERVAL=$(echo $SECONDS 50 | awk '{ print $1/$2 }')

	echo -e "\n${ANSI_CYAN}Waiting for container to be ready: ${ANSI_RESET}"
	
	for second in {1..50}
	do
		echo -ne "${ANSI_CYAN_BACKGROUND} ${ANSI_RESET}"
		sleep ${SLEEP_INTERVAL}
	done

	echo -e "${ANSI_CYAN} READY${ANSI_RESET}"
}


start_db () {
	export DB_VOL_CONTAINER=$1
	export DB_VERSION_FULL=$2

	docker run --name ${DB_VOL_CONTAINER} -d -p ${ZCX_PORT_DB}:5432 \
                -v ${DB_VOL_CONTAINER}:/var/lib/postgresql/data \
                -e POSTGRES_USER=artifactory -e POSTGRES_PASSWORD=password \
                icr.io/ibmz/postgres:${DB_VERSION_FULL} || return 1

        wait_for_container 5
        print_logs ${DB_VOL_CONTAINER}
}


start_artifactory () {
	docker run --name ${JFROG_VOL_CONTAINER} -d -p ${ZCX_PORT_ARTIFACTORY}:8081 \
                -v ${JFROG_VOL_CONTAINER}:/var/opt/jfrog/artifactory \
                -e DB_TYPE=postgresql -e DB_USER=artifactory -e DB_PASSWORD=password \
                -e DB_URL=jdbc:postgresql://${ZCX_HOST}:5431/artifactory \
                icr.io/ibmz/jfrog-artifactory-oss:${JFROG_VERSION} || return 1

        wait_for_container 90
        print_logs ${JFROG_VOL_CONTAINER}

	artifactory_status
}


artifactory_status() {
	export SUCCESS_STATUS=$(docker exec ${JFROG_VOL_CONTAINER} curl -i --fail localhost:8081/artifactory | grep HTTP/1.1) \
		|| return 1

        echo -e "\n${ANSI_GREEN}SUCCESS! Artifactory responded with:${ANSI_RESET}"
	echo -e "${ANSI_GREEN}${SUCCESS_STATUS}${ANSI_RESET}"
	echo -e  "${ANSI_GREEN}This indicates that Artifactory was able to successfully connect to the database.${ANSI_RESET}\n"
}



test_pg_upgrade () {
	test_start
	
	print_status "Creating volumes..."
	docker volume create ${JFROG_VOL_CONTAINER}
	docker volume create ${OLD_DB_VOL_CONTAINER}
	docker volume create ${NEW_DB_VOL_CONTAINER}
	
	print_status "Creating PostgreSQL ${OLD_DB_VERSION_FULL} database container..."
	start_db ${OLD_DB_VOL_CONTAINER} ${OLD_DB_VERSION_FULL} || return 1

	print_status "Initializing Artifactory container with PostgreSQL ${OLD_DB_VERSION_FULL} database..."
	start_artifactory || return 1

	print_status "Stopping Artifactory container..."
	docker stop ${JFROG_VOL_CONTAINER} && docker rm ${JFROG_VOL_CONTAINER}

	print_status "Stopping old version database container..."
	docker stop ${OLD_DB_VOL_CONTAINER}
	
	print_status "Perform upgrade"
	./perform-db-upgrade.sh ${OLD_DB_VOL_CONTAINER} ${NEW_DB_VOL_CONTAINER} ${OLD_DB_VERSION} ${NEW_DB_VERSION} || return 1
	
	print_status "Starting database container using upgraded PostgreSQL ${NEW_DB_VERSION_FULL} database..."
	start_db ${NEW_DB_VOL_CONTAINER} ${NEW_DB_VERSION_FULL} || return 1

	print_status "Creating Artifactory that uses state created using a PostgreSQL ${OLD_DB_VERSION_FULL} database. " + \
       		     "This Artifactory now should be able to use the upgraded PostgreSQL ${NEW_DB_VERSION_FULL} database..." 
	start_artifactory || return 1

	test_success
}


cleanup_containers () {
	print_status "Cleanup containers..."
	docker rm -f ${JFROG_VOL_CONTAINER}
	docker rm -f ${OLD_DB_VOL_CONTAINER}
	docker rm -f ${NEW_DB_VOL_CONTAINER}
}


cleanup_volumes () {	
	print_status "Cleanup volumes..."
	docker volume rm ${JFROG_VOL_CONTAINER}
        docker volume rm ${OLD_DB_VOL_CONTAINER}
        docker volume rm ${NEW_DB_VOL_CONTAINER}
}


test_pg_upgrade || test_failure
cleanup_containers || true
cleanup_volumes
