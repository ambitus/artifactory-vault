docker build . --tag artifactory-db2
docker run --name vault-db2 -d -p 8081:8081 -e DB_TYPE=db2 -e DB_USER=<userid> -e DB_PASSWORD=<password> -e DB_URL=<url endpoint to database> -e SKIP_DB_CHECK=true artifactory-db2
