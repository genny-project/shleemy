#!/bin/bash
./mvnw clean package -DskipTests=true
#./mvnw clean package -DskipTests=false
docker stop mysql-1
docker rm -f mysql-1
